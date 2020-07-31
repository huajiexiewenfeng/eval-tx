# eval-tx

基于 redis 的分布式事务框架

2PC 两阶段提交，原理简单易懂

附简单 Demo

## 原理图

![image-20200731094448889](https://github.com/huajiexiewenfeng/eval-tx/blob/master/images/image-20200731094448889.png)

## 介绍

* 目前已经应用用生产环境

* 使用 redis 来作为子事务的注册中心，保存各个子事务的状态，事务之前互相感知
* 所有事务要么一起提交，要么一起回滚
* 下阶段改造点
  * 注解
  * 多策略提交

## 环境

* spring-cloud Finchley+
* springboot 2.0+
* mysql
* redis

## 项目结构

* eval-eureka 注册中心
* eval-tx-core 核心代码实现
* eval-tx-demo 示例项目
  * eval-company 企业服务
  * eval-user 用户服务
  * eval-web 主调用服务

## 使用方式

1.主调用应用启动类增加 `@EnableEvalTransactionManager` 注解，激活分布式事务服务

```java
@MapperScan("com.csdn.dao")
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@EnableEvalTransactionManager
public class WebBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(WebBootstrap.class, args);
    }
}
```

2.依赖注入使用 `EvalTransactionManager` API

```java
    @Autowired
    private EvalTransactionManager evalTxManager;
```

3.开启主事务，调用 RPC 应用

* evalTxManager.beginEvalTransactionManager(); // 开启事务
* 获取全局事务ID，并传到 RPC 接口中
* evalTxManager.executeChildTask();// 执行 RPC 调用其他服务
* evalTxManager.commit();// 提交
* evalTxManager.rollback();// 回滚

```java
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
```

4.开启子事务，注意需要传入全局事务ID，通过开启主事务获取

* evalTxManager.beginChildEvalTransactionManager(globalTxId);// 开启子事务
* evalTxManager.commit();// 提交
* evalTxManager.rollback();// 回滚

```java
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
```

5.完成
