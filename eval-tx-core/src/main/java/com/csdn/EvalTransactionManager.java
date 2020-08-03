package com.csdn;

import com.csdn.util.RedisUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 分布式事务处理核心类
 * 采用 2PC 方式
 * 分布式应用开启本地事务并将 事务ID 注册到 redis 中
 * 分布式应用通过 全局事务ID 从 redis 中获取其它子事务的处理状态
 * - 默认超时策略：如果执行时间超过 60 秒，整个分布式事务集合没有执行完，那么回滚当前本地事务
 *
 * @author ：xwf
 * @date ：Created in 2020-6-15 16:48
 */
public class EvalTransactionManager {

    public static final String EVAL_TX_MANAGER_PREFIX = "eval_tx_manager_";

    public static final String EVAL_TX_MANAGER_COUNT_PREFIX = "eval_tx_manager_count_";// 事务计数器

    public static final String EVAL_TX_MANAGER_LIST = "eval_tx_manager_list_";

    public static final Integer DEFAULT_LOOP_COUNT = 600;//默认循环次数

    public static final Integer DEFAULT_LOOP_TIME = 100;// 默认循环时间

    private final Log logger = LogFactory.getLog(getClass());

    private DataSourceTransactionManager txManager;

    private RedisUtil redisUtil;

    private TransactionStatus status;

    private TimeoutExecutionHandler handler;// 超时提交策略

    private Integer loopCount = 0;

    private String transactionKey;// 当前子事务在 Redis 中的 key 值

    private String globalTransactionKey;// 全局事务，全局分布式事务在 Redis 中集合的 key 值

    private String globalTxId;// 全局事务ID

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    @PreDestroy
    public void destroy() {
        executorService.shutdown();
    }

    public EvalTransactionManager() {

    }

    public EvalTransactionManager(DataSourceTransactionManager txManager, RedisUtil redisUtil) {
        this.txManager = txManager;
        this.redisUtil = redisUtil;
    }

    /**
     * 开启事务
     *
     * @param globalId   全局事务id
     * @param id         子事务id
     * @param initialize 是否初始化
     */
    private void beginEvalTransactionManager(String globalId, String id, boolean initialize, TimeoutExecutionHandler handler) {
        this.status = txManager.getTransaction(new DefaultTransactionDefinition());
        this.transactionKey = EVAL_TX_MANAGER_PREFIX + id;
        this.globalTransactionKey = EVAL_TX_MANAGER_LIST + globalId;
        this.globalTxId = globalId;
        this.handler = handler;// 默认回滚
        if (initialize) {
            List<Object> keys = redisUtil.lGet(globalTransactionKey, 0, -1);
            for (int i = 0; i < keys.size(); i++) {
                redisUtil.del(String.valueOf(keys.get(i)));
            }
            redisUtil.del(globalTransactionKey);
        }
        add();//新建本地事务，默认将本地事务加添到分布式事务集合中
    }

    /**
     * 开启全局事务
     *
     * @param handler 超时策略
     * @return
     */
    public String beginEvalTransactionManager(TimeoutExecutionHandler handler) {
        String id = getId();
        String globalId = getId();
        beginEvalTransactionManager(globalId, id, true, handler);
        redisUtil.set(EVAL_TX_MANAGER_COUNT_PREFIX + globalId, 1);// 增加子事务数
        return globalId;
    }

    public String beginEvalTransactionManager() {
        return beginEvalTransactionManager(new DefaultRollbackPolicy());
    }

    /**
     * 开启全局事务
     *
     * @param timeout 超时时间 s 秒
     * @param handler 超时策略
     * @return
     */
    public String beginEvalTransactionManager(Integer timeout, TimeoutExecutionHandler handler) {
        this.loopCount = timeout * 10;
        return beginEvalTransactionManager(handler);
    }

    public String beginEvalTransactionManager(Integer timeout) {
        this.loopCount = timeout * 10;
        return beginEvalTransactionManager(new DefaultRollbackPolicy());
    }

    /**
     * 开启子事务
     *
     * @param globalId 全局事务编号
     * @param handler  超时策略
     * @return
     */
    public String beginChildEvalTransactionManager(String globalId, TimeoutExecutionHandler handler) {
        String id = getId();
        beginEvalTransactionManager(globalId, id, false, handler);
        return id;
    }

    public String beginChildEvalTransactionManager(String globalId, Integer timeout, TimeoutExecutionHandler handler) {
        this.loopCount = timeout * 10;
        return beginChildEvalTransactionManager(globalId, handler);
    }

    public String beginChildEvalTransactionManager(String globalId) {
        return beginChildEvalTransactionManager(globalId, new DefaultRollbackPolicy());
    }

    /**
     * 开启子事务
     *
     * @param globalId 全局事务编号
     * @param timeout  超时时间 s 秒
     * @return
     */
    public String beginChildEvalTransactionManager(String globalId, Integer timeout) {
        this.loopCount = timeout * 10;
        return beginChildEvalTransactionManager(globalId, new DefaultRollbackPolicy());
    }

    private String getId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 使用线程池来执行 RPC 请求任务
     *
     * @param r
     */
    public void executeChildTask(Runnable r) {
        // 增加子事务数
        redisUtil.incr(EVAL_TX_MANAGER_COUNT_PREFIX + globalTxId, 1);
        executorService.execute(r);
    }

    /**
     * 使用线程池来执行 RPC 请求任务(带返回值)
     *
     * @param c
     */
    public Future submitChildTask(Callable c) {
        // 增加子事务数
        redisUtil.incr(EVAL_TX_MANAGER_COUNT_PREFIX + globalTxId, 1);
        return executorService.submit(c);
    }

    /**
     * 回滚事务
     */
    public void rollback() {
        logger.warn("事务回滚,事务id:[" + transactionKey + "]");
        redisUtil.set(transactionKey, 0); // 设置 redis 中对应的事务 id 的值为 0 表示失败
        txManager.rollback(status);
    }

    /**
     * 提交事务
     * 1.超时策略选择
     * 1.1 回退
     * 1.2 超过 2/3 提交
     * 1.3 最终提交，但是记录错误日志
     * 改造点：
     * 超时时间的计算判断不精确
     *
     * @return
     * @throws InterruptedException
     */
    public boolean commit() throws InterruptedException {
        int txCount = getTxCount(globalTxId);// 获取子事务数
        int timeoutLoopCount = loopCount > 0 ? loopCount : DEFAULT_LOOP_COUNT;
        int keySize = 0;
        List<Object> keys = new ArrayList<>();
        int max_count = 0;
        redisUtil.set(transactionKey, 1); // 设置 redis 中对应的事务 id 的值为 1 表示当前事务预处理成功
        boolean flag = true;// 默认事务执行成功
        while (true) {
            // 获取当前分布式事务中所有的本地事务 key
            keys = redisUtil.lGet(globalTransactionKey, 0, -1);
            keySize = keys.size();
            if (txCount == 0 || keySize == 0) {
                flag = false;
                break;
            }
            if (keySize < txCount) {
                continue;
            }
            int result = 0;
            for (int i = 0; i < keys.size(); i++) {
                String key = String.valueOf(keys.get(i));
                String transactionStatus = String.valueOf(redisUtil.get(key));// 获取key的状态
                if ("1".equals(transactionStatus)) {// 如果其他子事务的状态为1 表示执行成功
                    result++;
                }
                if ("0".equals(transactionStatus)) {// 其他子事务执行失败
                    logger.warn("有其他子事务执行失败,回滚当前子事务，事务id:[" + transactionKey + "]");
                    flag = false; // 跳出 for 循环
                    break;
                }
            }
            if (!flag) {// 执行完 for 循环，如果 flag = false,表示有事务执行失败
                break;// 直接退出自旋
            }
            if (result == keys.size()) {// 当前分布式事务中所有的子事务都执行成功
                logger.info("所有事务完成,提交当前子事务，事务id:[" + transactionKey + "]");
                // 提交事务
                flag = true;
                break;// 退出自旋
            }
            // 每次休息 100 毫秒，循环 600 次，总计 60 秒
            // 中间少计算了 从 redis 获取事务集合和子事务状态的时间 比如设置5秒超时，实际上，可能 10 秒才能感知到
            if (max_count <= timeoutLoopCount) {
                max_count++;
            } else {
                flag = handler.timeoutExecution(result, keys.size(), transactionKey);
                break;// 退出自旋
            }
            Thread.sleep(DEFAULT_LOOP_TIME);
        }

        if (flag) {
            txManager.commit(status);
        } else {
            redisUtil.set(transactionKey, 0);
            txManager.rollback(status);
        }

        return flag;
    }

    /**
     * 获取当前全局事务的子事务数
     *
     * @param globalTxId
     * @return
     */
    private int getTxCount(String globalTxId) {
        String key = EVAL_TX_MANAGER_COUNT_PREFIX + globalTxId;
        if (redisUtil.hasKey(key)) {
            return Integer.valueOf(String.valueOf(redisUtil.get(key)));
        } else {
            return 0;
        }
    }

    private void add() {
        // 每创建一个本地事务，将该事务的 id 放到当前分布式事务的集合中
        redisUtil.lSet(globalTransactionKey, transactionKey);
    }

}
