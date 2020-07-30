package com.csdn.config;

import com.csdn.EvalTransactionManager;
import com.csdn.util.RedisUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.csdn.EvalTransactionManagerMarkerConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import redis.clients.jedis.JedisPoolConfig;

import javax.sql.DataSource;

/**
 * EvalTransactionManager自动配置类
 *
 * @author ：xwf
 * @date ：Created in 2020-6-16 9:45
 */
@Configuration
@ConditionalOnBean(EvalTransactionManagerMarkerConfiguration.Marker.class)
@EnableConfigurationProperties
public class EvalTransactionManagerAutoConfiguration {

    @Bean
    public DataSourceConfig dataSourceConfig() {
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        return dataSourceConfig;
    }

    @Bean
    public JedisConfig jedisConfig() {
        JedisConfig jedisConfig = new JedisConfig();
        return jedisConfig;
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceConfig dataSourceConfig) {
        HikariDataSource datasource = new HikariDataSource();
        datasource.setJdbcUrl(dataSourceConfig.getUrl());
        datasource.setUsername(dataSourceConfig.getUsername());
        datasource.setPassword(dataSourceConfig.getPassword());
        datasource.setDriverClassName(dataSourceConfig.getDriverClassName());
        datasource.setMaximumPoolSize(dataSourceConfig.getMaxActive());
        datasource.setMinimumIdle(dataSourceConfig.getMinIdle());
        return datasource;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public EvalTransactionManager evalTxManager(DataSourceTransactionManager txManager, RedisUtil redisUtil) {
        return new EvalTransactionManager(txManager, redisUtil);
    }

    @Bean
    @ConditionalOnMissingBean
    public JedisConnectionFactory jedisConnectionFactory(JedisConfig jedisConfig) {
        RedisStandaloneConfiguration rf = new RedisStandaloneConfiguration();
        rf.setDatabase(jedisConfig.getDatabase());
        rf.setHostName(jedisConfig.getHost());
        rf.setPort(jedisConfig.getPort());
        rf.setPassword(RedisPassword.of(jedisConfig.getPassword()));

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(50);
        jedisPoolConfig.setMinIdle(8);
        jedisPoolConfig.setMaxTotal(200);
        jedisPoolConfig.setMaxWaitMillis(10000);

        JedisClientConfiguration.JedisPoolingClientConfigurationBuilder jpb =
                (JedisClientConfiguration.JedisPoolingClientConfigurationBuilder) JedisClientConfiguration.builder();
        jpb.poolConfig(jedisPoolConfig);

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(rf, jpb.build());

        return jedisConnectionFactory;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate redisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(jedisConnectionFactory);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

}
