package com.csdn.controller;

import com.csdn.DefaultRollbackPolicy;
import com.csdn.EvalTransactionManager;
import com.csdn.MostCommitPolicy;
import com.csdn.annotation.EvalTransactional;
import com.csdn.dao.WebMapper;
import com.csdn.service.CompanyFeignClient;
import com.csdn.service.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author ：xwf
 * @date ：Created in 2020-7-24 16:56
 */
@RestController
public class UserController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CompanyFeignClient companyFeignClient;

    @Autowired
    private WebMapper webMapper;

    @Autowired
    private EvalTransactionManager evalTxManager;

    /**
     * 非事务版本
     *
     * @return
     */
    @GetMapping("addUser")
    public String addUser() {
        // RPC 调用用户服务增加用户
        String a = userFeignClient.addUser("1", "user");
        // RPC 调用企业服务增加企业
        String b = companyFeignClient.addCompany("1", "company");
        // 插入本地数据库成功标识
        webMapper.add("1", "success");
        return a + b;
    }

    /**
     * 分布式事务->正常演示
     *
     * @return
     */
    @GetMapping("addUserTx")
    public String addUserTx() {
        String globalTxId = evalTxManager.beginEvalTransactionManager();
        try {
            // RPC 调用用户服务增加用户
            evalTxManager.executeChildTask(() -> {
                userFeignClient.addUserTx(globalTxId, "1", "user");
            });

            // RPC 调用企业服务增加企业
            evalTxManager.executeChildTask(() -> {
                companyFeignClient.addCompanyTx(globalTxId, "1", "company");
            });

            // 插入本地数据库成功标识
            webMapper.add("1", "success");
            evalTxManager.commit();
        } catch (Exception e) {
            e.printStackTrace();
            evalTxManager.rollback();
        }
        return "";
    }

    /**
     * 分布式事务->异常演示
     *
     * @return
     */
    @GetMapping("addUserTxException")
    public String addUserTxException() {
        String globalTxId = evalTxManager.beginEvalTransactionManager();
        try {
            // RPC 调用用户服务增加用户
            evalTxManager.executeChildTask(() -> {
                userFeignClient.addUserTx(globalTxId, "1", "user");
            });

            // RPC 调用企业服务增加企业
            evalTxManager.executeChildTask(() -> {
                companyFeignClient.addCompanyTxException(globalTxId, "1", "company");
            });

            // 插入本地数据库成功标识
            webMapper.add("1", "success");
            evalTxManager.commit();
        } catch (Exception e) {
            e.printStackTrace();
            evalTxManager.rollback();
        }
        return "";
    }

    /**
     * 分布式事务->超时时间演示 5 秒+超时策略
     *
     * @return
     */
    @GetMapping("addUserTxTimeout")
    public String addUserTxTimeout() {
        // DefaultRollbackPolicy FinalCommitPolicy MostCommitPolicy
        String globalTxId = evalTxManager.beginEvalTransactionManager(5, new MostCommitPolicy());// 设置超时时间为5秒
        try {
            // RPC 调用用户服务增加用户
            evalTxManager.executeChildTask(() -> {
                userFeignClient.addUserTx(globalTxId, "1", "user");
            });

            // RPC 调用企业服务增加企业
            evalTxManager.executeChildTask(() -> {
                companyFeignClient.addCompanyTxTimeout(globalTxId, "1", "company");
            });

            // 插入本地数据库成功标识
            webMapper.add("1", "success");
            evalTxManager.commit();
        } catch (Exception e) {
            e.printStackTrace();
            evalTxManager.rollback();
        }
        return "";
    }

    /**
     * 分布式事务-> 注解 Annotation
     * 超时时间演示 5 秒+超时策略（多数提交）
     *
     * @return
     */
    @GetMapping("addUserTxTimeoutAnnotation")
    @EvalTransactional(timeoutSeconds = 5, timeoutHandler = MostCommitPolicy.class)
    public String addUserTxTimeoutAnnotation(String globalTxId) {

        // RPC 调用用户服务增加用户
        evalTxManager.executeChildTask(() -> {
            userFeignClient.addUserTxAnnotation(globalTxId, "1", "user");
        });

        // RPC 调用企业服务增加企业
        evalTxManager.executeChildTask(() -> {
            companyFeignClient.addCompanyTxTimeoutAnnotation(globalTxId, "1", "company");
        });

        // 插入本地数据库成功标识
        webMapper.add("1", "success");
        return "";
    }

    @GetMapping("addUserTxAnnotation")
    @EvalTransactional(timeoutSeconds = 5,timeoutHandler = DefaultRollbackPolicy.class)
    public String addUserTxAnnotation(String globalTxId) {

        // RPC 调用用户服务增加用户
        evalTxManager.executeChildTask(() -> {
            userFeignClient.addUserTxAnnotation(globalTxId, "1", "user");
        });

        // RPC 调用企业服务增加企业
        evalTxManager.executeChildTask(() -> {
            companyFeignClient.addCompanyTxTimeoutAnnotation(globalTxId, "1", "company");
        });

        // 插入本地数据库成功标识
        webMapper.add("1", "success");
        return "";
    }
}
