package com.csdn;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.TimeoutException;

/**
 * 回滚（默认策略）
 *
 * @author ：xwf
 * @date ：Created in 2020-8-1 9:26
 */
public class DefaultRollbackPolicy implements TimeoutExecutionHandler {

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public boolean timeoutExecution(Integer successCount, Integer sumCount, String transactionKey) {
        if (ObjectUtils.nullSafeEquals(successCount, sumCount)) {
            logger.info("所有事务完成,提交当前子事务，事务id:[" + transactionKey + "]");
            return true;
        } else {
            logger.warn("等待超时后，还有事务没有完成，回滚当前子事务，总事务数:[" + sumCount + "]，当前完成事务数:[" + successCount + "]，当前事务id:[" + transactionKey + "]",
                    new TimeoutException("当前事务[" + transactionKey + "]执行超时"));
            return false;
        }
    }

}
