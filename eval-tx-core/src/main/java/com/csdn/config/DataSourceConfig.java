package com.csdn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据库配置
 *
 * @author ：xwf
 * @date ：Created in 2020-7-30 14:54
 */
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceConfig {

    private String url;

    private String username;

    private String password;

    private String driverClassName = "com.mysql.jdbc.Driver";

    private Integer maxActive = 20;

    private Integer minIdle = 1;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public Integer getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(Integer maxActive) {
        this.maxActive = maxActive;
    }

    public Integer getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(Integer minIdle) {
        this.minIdle = minIdle;
    }
}
