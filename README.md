# eval-tx

基于 redis 的分布式事务框架

2PC 两阶段提交，原理简单易懂

附简单 Demo

博客地址：[https://blog.csdn.net/xiewenfeng520/article/details/107710739](https://blog.csdn.net/xiewenfeng520/article/details/107710739)

## 特点

* 侵入性低
* 自动装配
* @Enable注解激活，开箱即用
* 基于 Spring 事务进行二次封装，学习成本低
* 支持超时时间+超时策略
* 支持注解，使用方便

## 原理图

![image-20200731094448889](https://github.com/huajiexiewenfeng/eval-tx/blob/master/images/image-20200803154118223.png)

业务流程：

* 服务A 操作数据库1

* 服务A 调用 服务B 操作数据库2

* 服务A 调用 服务C 操作数据库3

业务伪代码：

```java
method(){
    globalId = "A";// 全局事务编号
    beginTransaction();// 开启事务
    RPC-method2(globalId,2);// 远程RPC调用，操作数据库2
    RPC-method3(globalId,3);// 远程RPC调用，操作数据库3
    method1(1);// 操作数据库1
}
```

要达到的效果：

要么三个操作一起成功，要么一起失败。

## 介绍

* 使用 redis 来作为子事务的注册中心，保存各个子事务的状态，事务之前互相感知
* 所有事务要么一起提交，要么一起回滚
* 新增 @EvalTransactional 注解，支持超时时间和超时策略设置（内置三种策略）
* 下阶段改造
  * 超时时间精确度
  * 子线程返回值处理

## 环境

* spring-cloud Finchley+
* springboot 2.0+
* mysql
* redis

## 项目结构

* eval-eureka 注册中心
* eval-tx-core 核心代码实现（实际项目依赖此模块即可）
* eval-tx-demo 示例项目
  * eval-company 企业服务
  * eval-user 用户服务
  * eval-web 主调用服务

## 使用方式

### API 编程

1.启动类增加 `@EnableEvalTransactionManager` 注解，激活分布式事务服务

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

2.依赖注入即可使用 `EvalTransactionManager` API

```java
    @Autowired
    private EvalTransactionManager evalTxManager;
```

3.开启主事务，调用 RPC 应用

* evalTxManager.beginEvalTransactionManager(); // 开启事务
* 获取全局事务ID，并传到 RPC 接口中
* evalTxManager.executeChildTask();// 执行 RPC 调用其他服务
  * 也支持 Future，可以获取返回值
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

4.开启子事务，注意需要传入全局事务ID(开启主事务获取)

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

### Annotation 注解

1.启动类增加 `@EnableEvalTransactionManager` 注解，激活分布式事务服务

2.开启主事务

* 对应方法加上 `@EvalTransactional` 注解

* 方法入参需要加上 `String globalTxId`，框架会做处理
* timeoutSeconds 默认值 60 秒
* 默认超时策略是回滚 DefaultRollbackPolicy

```java
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
```

3.开启子事务

* 对应方法加上 `@EvalTransactional(type = EvalTransactionalConstants.TYPE_CHILD)` 注解，type 的值 `child` 表示子事务

* 方法入参需要加上 `String globalTxId`，框架会做处理

```java
    @PostMapping(value = "/eval-user/api/addUserTxAnnotation")
    @EvalTransactional(type = EvalTransactionalConstants.TYPE_CHILD)
    public String addUserTxAnnotation(String globalTxId, String id, String name) {
        userMapper.addUser(id, name);
        return "success";
    }
```

4.完成

## 高级特性

### 超时时间设置

可以通过两种方式进行设置

* API，表示超时时间为 5 秒

```java
evalTxManager.beginEvalTransactionManager(5);
```

* Annotation 注解，表示超时时间为 5 秒

```java
@EvalTransactional(timeoutSeconds = 5)
```

### 超时策略选择&自定义

* API，表示超时时间为 5 秒，超时策略为大多数提交（2/3 以上提交）

```java
evalTxManager.beginEvalTransactionManager(5, timeoutHandler = MostCommitPolicy.class);
```

* Annotation 注解，表示超时时间为 5 秒，超时策略为大多数提交（2/3 以上提交）

```java
@EvalTransactional(timeoutSeconds = 5, timeoutHandler = MostCommitPolicy.class)
```

目前内置三种策略

* DefaultRollbackPolicy 默认策略，超时后回滚当前事务
* MostCommitPolicy 2/3 以上提交成功，提交当前事务
* FinalCommitPolicy 超时之后提交当前事务，记录相关日志

自定义超时策略

* 实现 TimeoutExecutionHandler 接口即可

```java
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
```

使用方式和其他策略一样，在注解或者 API 中直接使用即可。

```java
 @EvalTransactional(timeoutHandler = CustomedCommitPolicy.class)
```