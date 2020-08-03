package com.csdn.rpc;

import com.csdn.TimeoutExecutionHandler;


/**
 * @author ：xwf
 * @date ：Created in 2020-8-3 15:55
 */
public class CustomedCommitPolicy implements TimeoutExecutionHandler {

    /**
     * @param successCount   成功的子事务数
     * @param sumCount       总事务数
     * @param transactionKey 当前事务ID
     * @return false : 回滚 ，true : 提交
     */
    @Override
    public boolean timeoutExecution(Integer successCount, Integer sumCount, String transactionKey) {
        System.out.println("自定义超时策略");
        return false;
    }
}
