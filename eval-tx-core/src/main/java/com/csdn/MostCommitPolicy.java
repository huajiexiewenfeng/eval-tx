package com.csdn;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.TimeoutException;

/**
 * 大多数提交策略
 * 超过 2/3 的事务提交成功，那么提交本地事务
 *
 * @author ：xwf
 * @date ：Created in 2020-8-1 9:26
 */
public class MostCommitPolicy implements TimeoutExecutionHandler {

    private final Log logger = LogFactory.getLog(getClass());

    private double ratio;

    public MostCommitPolicy() {
        ratio = (double) 2 / (double) 3;
    }

    public MostCommitPolicy(double ratio) {
        if (ratio >= 1) {
            ratio = 1;
        }
        this.ratio = ratio;
    }

    @Override
    public boolean timeoutExecution(Integer successCount, Integer sumCount, String transactionKey) {
        if (successCount >= sumCount * ratio) {
            logger.info("大多数事务完成，提交当前子事务，总事务数:[" + sumCount + "]，当前完成事务数:[" + successCount + "]，当前事务id:[" + transactionKey + "]");
            return true;
        } else {
            logger.warn("等待超时后，还有事务没有完成，回滚当前子事务，总事务数:[" + sumCount + "]，当前完成事务数:[" + successCount + "]，当前事务id:[" + transactionKey + "]",
                    new TimeoutException("当前事务[" + transactionKey + "]执行超时"));
            return false;
        }
    }

}
