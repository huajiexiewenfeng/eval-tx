package com.csdn;


/**
 * 超时策略处理者
 *
 * @author ：xwf
 * @date ：Created in 2020-8-1 9:23
 */
public interface TimeoutExecutionHandler {

    /**
     * 超时执行
     *
     * @param successCount   成功的子事务数
     * @param sumCount       总事务数
     * @param transactionKey 当前事务ID
     * @return
     */
    boolean timeoutExecution(Integer successCount, Integer sumCount, String transactionKey);
}
