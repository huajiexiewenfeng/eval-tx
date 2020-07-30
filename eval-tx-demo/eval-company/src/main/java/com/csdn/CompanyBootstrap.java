package com.csdn;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 示例启动引导类
 *
 * @author ：xwf
 * @date ：Created in 2020-7-24 16:52
 */
@MapperScan("com.csdn.dao")
@SpringBootApplication
@EnableDiscoveryClient
public class CompanyBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(CompanyBootstrap.class, args);
    }
}
