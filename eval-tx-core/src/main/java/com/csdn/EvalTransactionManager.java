package com.csdn;

import com.csdn.util.RedisUtil;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 用来处理考评系统的分布式事务
 * 采用 2PC 方式
 * - 如果执行时间超过 60 秒，整个分布式事务集合没有执行完，那么回滚当前本地事务
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

    private DataSourceTransactionManager txManager;

    private RedisUtil redisUtil;

    private TransactionStatus status;

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
     * @param globalId   全局事务id
     * @param id         子事务id
     * @param initialize 是否初始化
     */
    private void beginEvalTransactionManager(String globalId, String id, boolean initialize) {
        status = txManager.getTransaction(new DefaultTransactionDefinition());
        transactionKey = EVAL_TX_MANAGER_PREFIX + id;
        globalTransactionKey = EVAL_TX_MANAGER_LIST + globalId;
        globalTxId = globalId;

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
     * @return
     */
    public String beginEvalTransactionManager() {
        String id = getId();
        String globalId = getId();
        beginEvalTransactionManager(globalId, id, true);
        redisUtil.set(EVAL_TX_MANAGER_COUNT_PREFIX + globalId, 1);// 增加子事务数
        return globalId;
    }

    /**
     * 开启子事务
     *
     * @param globalId
     * @return
     */
    public String beginChildEvalTransactionManager(String globalId) {
        String id = getId();
        beginEvalTransactionManager(globalId, id, false);
        return id;
    }

    private String getId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void executeChildTask(Runnable r) {
        // 增加子事务数
        redisUtil.incr(EVAL_TX_MANAGER_COUNT_PREFIX + globalTxId, 1);
        executorService.execute(r);
    }

    public Future submitChildTask(Callable c) {
        // 增加子事务数
        redisUtil.incr(EVAL_TX_MANAGER_COUNT_PREFIX + globalTxId, 1);
        return executorService.submit(c);
    }

    public void rollback() {
        System.out.printf("事务回滚,事务id:[%s]\n", transactionKey);
        redisUtil.set(transactionKey, 0); // 设置 redis 中对应的事务 id 的值为 0 表示失败
        txManager.rollback(status);
    }

    public boolean commit() throws InterruptedException {
        int txCount = getTxCount(globalTxId);// 获取子事务数
        int max_count = 0;
        redisUtil.set(transactionKey, 1); // 设置 redis 中对应的事务 id 的值为 1 表示当前事务预处理成功
        boolean flag = true;// 默认事务执行成功
        while (true) {
            // 获取当前分布式事务中所有的本地事务 key
            List<Object> keys = redisUtil.lGet(globalTransactionKey, 0, -1);
            if (txCount == 0 || keys.size() == 0) {
                flag = false;
                break;
            }
            if (keys.size() < txCount) {
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
                    System.out.printf("有其他子事务执行失败,回滚当前子事务，事务id:[%s]\n", transactionKey);
                    flag = false; // 跳出 for 循环
                    break;
                }
            }
            if (!flag) {// 执行完 for 循环，如果 flag = false,表示有事务执行失败
                break;// 直接退出自旋
            }
            if (result == keys.size()) {// 当前分布式事务中所有的子事务都执行成功
                System.out.printf("所有事务完成,提交当前子事务，事务id:[%s]\n", transactionKey);
                // 提交事务
                flag = true;
                break;// 退出自旋
            }
            if (max_count <= DEFAULT_LOOP_COUNT) {// 每次休息 100 毫秒，循环 600 次，总计 60 秒
                max_count++;
            } else {
                System.out.printf("循环60秒后，还有事务没有完成,回滚当前子事务，事务id:[%s]\n", transactionKey);
                flag = false;
                break;// 退出自旋
            }
            Thread.sleep(DEFAULT_LOOP_TIME);
        }

        if (flag) {
            txManager.commit(status);
        } else {
            txManager.rollback(status);
        }
        return flag;
    }

    private int getTxCount(String globalTxId) {
        String key = EVAL_TX_MANAGER_COUNT_PREFIX + globalTxId;
        if (redisUtil.hasKey(key)) {
            return Integer.valueOf(String.valueOf(redisUtil.get(key)));
        } else {
            return 0;
        }

    }

    public void add() {
        // 每创建一个本地事务，将该事务的 id 放到当前分布式事务的集合中
        redisUtil.lSet(globalTransactionKey, transactionKey);
    }
}
