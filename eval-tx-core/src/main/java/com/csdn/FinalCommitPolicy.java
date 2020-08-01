package com.csdn;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * 最终提交策略
 * 即使其他事务未执行完成，最终还是提交本地事务
 * 可能有个别服务执行时间非常长，比如数据库大数据量的批量操作，
 * 如果回滚事务，数据库又需要同样的时间来进行回滚操作，得不偿失
 *
 * @author ：xwf
 * @date ：Created in 2020-8-1 9:26
 */
public class FinalCommitPolicy implements TimeoutExecutionHandler {

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public boolean timeoutExecution(Integer successCount, Integer sumCount, String transactionKey) {
        logger.warn("等待超时后，还有事务没有完成，提交当前子事务，总事务数:[" + sumCount + "]，当前完成事务数:[" + successCount + "]，当前事务id:[" + transactionKey + "]");
        return true;
    }

}
