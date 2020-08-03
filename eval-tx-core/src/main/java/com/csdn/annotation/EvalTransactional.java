package com.csdn.annotation;

import com.csdn.DefaultRollbackPolicy;
import com.csdn.TimeoutExecutionHandler;
import com.csdn.constants.EvalTransactionalConstants;

import java.lang.annotation.*;

@Target({ElementType.METHOD,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface EvalTransactional {

    String type() default EvalTransactionalConstants.TYPE_GLOBAL;// global:全局事务 child:子事务

    int timeoutSeconds() default 60;// 超时时间 秒

    Class<? extends TimeoutExecutionHandler> timeoutHandler() default DefaultRollbackPolicy.class;// 超时处理策略

}
