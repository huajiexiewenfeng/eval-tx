package com.csdn.rpc;

import com.csdn.EvalTransactionManager;
import com.csdn.annotation.EvalTransactional;
import com.csdn.constants.EvalTransactionalConstants;
import com.csdn.dao.CompanyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：xwf
 * @date ：Created in 2020-7-24 17:21
 */
@RestController
public class CompanyFeignClient {

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private EvalTransactionManager evalTxManager;

    @PostMapping(value = "/eval-company/api/addCompany")
    public String addCompany(String id, String name) {
        companyMapper.addCompany(id, name);
        return "success";
    }

    @PostMapping(value = "/eval-company/api/addCompanyTx")
    public String addCompanyTx(String globalTxId, String id, String name) {
        evalTxManager.beginChildEvalTransactionManager(globalTxId);
        try {
            companyMapper.addCompany(id, name);
            evalTxManager.commit();
        } catch (Exception e) {
            evalTxManager.rollback();
            return "fail";
        }
        return "success";
    }


    @PostMapping(value = "/eval-company/api/addCompanyTxException")
    public String addCompanyTxException(String globalTxId, String id, String name) {
        evalTxManager.beginChildEvalTransactionManager(globalTxId);
        try {
            companyMapper.addCompany(id, name);
            doException();
            evalTxManager.commit();
        } catch (Exception e) {
            evalTxManager.rollback();
            return "fail";
        }
        return "success";
    }

    public void doException() throws Exception {
        throw new Exception("故意抛出异常");
    }


    /**
     * 超时演示
     *
     * @param globalTxId
     * @param id
     * @param name
     * @return
     */
    @PostMapping(value = "/eval-company/api/addCompanyTxTimeout")
    public String addCompanyTxTimeout(String globalTxId, String id, String name) {
        evalTxManager.beginChildEvalTransactionManager(globalTxId);// 设置超时时间
        try {
            companyMapper.addCompany(id, name);
            Thread.sleep(15000);
            evalTxManager.commit();
        } catch (Exception e) {
            evalTxManager.rollback();
            return "fail";
        }
        return "success";
    }

    /**
     * 超时演示-Annotation
     *
     * @param globalTxId
     * @param id
     * @param name
     * @return
     */
    @PostMapping(value = "/eval-company/api/addCompanyTxTimeoutAnnotation")
    @EvalTransactional(type = EvalTransactionalConstants.TYPE_CHILD)
    public String addCompanyTxTimeoutAnnotation(String globalTxId, String id, String name) throws InterruptedException {
        companyMapper.addCompany(id, name);
        Thread.sleep(15000);
        return "success";
    }
}
