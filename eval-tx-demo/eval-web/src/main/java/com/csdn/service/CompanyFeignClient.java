package com.csdn.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ：xwf
 * @date ：Created in 2020-7-24 17:15
 */
@FeignClient(value = "eval-company")
public interface CompanyFeignClient {

    @PostMapping(value = "/eval-company/api/addCompany")
    String addCompany(@RequestParam("id") String id, @RequestParam("name") String name);

    @PostMapping(value = "/eval-company/api/addCompanyTx")
    String addCompanyTx(@RequestParam("globalTxId") String globalTxId, @RequestParam("id") String id, @RequestParam("name") String name);

    @PostMapping(value = "/eval-company/api/addCompanyTxException")
    String addCompanyTxException(@RequestParam("globalTxId") String globalTxId, @RequestParam("id") String id, @RequestParam("name") String name);

    @PostMapping(value = "/eval-company/api/addCompanyTxTimeout")
    String addCompanyTxTimeout(@RequestParam("globalTxId") String globalTxId, @RequestParam("id") String id, @RequestParam("name") String name);

    @PostMapping(value = "/eval-company/api/addCompanyTxTimeoutAnnotation")
    String addCompanyTxTimeoutAnnotation(@RequestParam("globalTxId") String globalTxId, @RequestParam("id") String id, @RequestParam("name") String name);
}
