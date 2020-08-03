package com.csdn.rpc;

import com.csdn.EvalTransactionManager;
import com.csdn.annotation.EvalTransactional;
import com.csdn.constants.EvalTransactionalConstants;
import com.csdn.dao.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：xwf
 * @date ：Created in 2020-7-24 17:21
 */
@RestController
public class UserFeignClient {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EvalTransactionManager evalTxManager;

    @PostMapping(value = "/eval-user/api/addUser")
    public String addUser(String id, String name) {
        userMapper.addUser(id, name);
        return "success";
    }

    /**
     * 改造点：
     * 1.@EvalTransactional(type="child") 或者 @EvalChildTransactional 表示是处理子事务
     * 2.txManager 和 redisUtil 不需要作为参数进行传递，是否可以放在 AutoConfiguration 中
     *
     * @param globalTxId
     * @param id
     * @param name
     * @return
     */
    @PostMapping(value = "/eval-user/api/addUserTx")
    public String addUserTx(String globalTxId, String id, String name) {
        evalTxManager.beginChildEvalTransactionManager(globalTxId);
        try {
            userMapper.addUser(id, name);
            evalTxManager.commit();
        } catch (Exception e) {
            evalTxManager.rollback();
            return "fail";
        }
        return "success";
    }

    @PostMapping(value = "/eval-user/api/addUserTxAnnotation")
    @EvalTransactional(type = EvalTransactionalConstants.TYPE_CHILD)
    public String addUserTxAnnotation(String globalTxId, String id, String name) {
        userMapper.addUser(id, name);
        return "success";
    }

    @PostMapping(value = "/eval-user/api/addUserTxAnnotationCustom")
    @EvalTransactional(type = EvalTransactionalConstants.TYPE_CHILD,timeoutHandler = CustomedCommitPolicy.class)
    public String addUserTxAnnotationCustom(String globalTxId, String id, String name) {
        userMapper.addUser(id, name);
        return "success";
    }
}
