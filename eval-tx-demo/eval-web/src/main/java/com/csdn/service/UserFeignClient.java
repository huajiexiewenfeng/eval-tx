package com.csdn.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ：xwf
 * @date ：Created in 2020-7-24 17:15
 */
@FeignClient(value = "eval-user")
public interface UserFeignClient {

    @PostMapping("/eval-user/api/addUser")
    String addUser(@RequestParam("id") String id, @RequestParam("name") String name);

    @PostMapping("/eval-user/api/addUserTx")
    String addUserTx(@RequestParam("globalTxId") String globalTxId, @RequestParam("id") String id, @RequestParam("name") String name);

    @PostMapping("/eval-user/api/addUserTxAnnotation")
    String addUserTxAnnotation(@RequestParam("globalTxId") String globalTxId, @RequestParam("id") String id, @RequestParam("name") String name);

}
