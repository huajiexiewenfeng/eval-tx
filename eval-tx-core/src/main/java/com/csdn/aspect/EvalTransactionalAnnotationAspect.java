package com.csdn.aspect;

import com.csdn.EvalTransactionManager;
import com.csdn.TimeoutExecutionHandler;
import com.csdn.annotation.EvalTransactional;
import com.csdn.constants.EvalTransactionalConstants;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * {@link EvalTransactional} 切面类
 * 1.处理超时时间
 * 2.起开全局事务
 * 3.开启子事务
 * 4.超时策略初始化
 *
 * @author ：xwf
 * @date ：Created in 2020-8-3 10:36
 */
@Aspect
@Component
public class EvalTransactionalAnnotationAspect {

    @Autowired
    private EvalTransactionManager evalTxManager;

    @Pointcut("@annotation(com.csdn.annotation.EvalTransactional)")
    void anyTransactionalAnnotatedMethodCall() {
    }

    @Around("anyTransactionalAnnotatedMethodCall()")
    public Object executeAnnotatedMethod(ProceedingJoinPoint aJoinPoint) throws Throwable {
        BeforeAdviceMethodInvocationAdapter mi = BeforeAdviceMethodInvocationAdapter.createFrom(aJoinPoint);
        Method method = mi.getMethod();
        Object[] args = mi.getArguments();
        Object res = null;
        if (method.isAnnotationPresent(EvalTransactional.class)) {
            EvalTransactional annotation = method.getAnnotation(EvalTransactional.class);
            int timeoutSeconds = annotation.timeoutSeconds();
            String type = annotation.type();
            Class handlerClass = annotation.timeoutHandler();
            TimeoutExecutionHandler handler = (TimeoutExecutionHandler) handlerClass.newInstance();
            try {
                if (ObjectUtils.nullSafeEquals(EvalTransactionalConstants.TYPE_GLOBAL, type)) {// 全局事务处理
                    String globalTxId = evalTxManager.beginEvalTransactionManager(timeoutSeconds, handler);
                    args[this.getGlobalTxIdIndex(args, aJoinPoint)] = globalTxId;// 默认参数名称为 globalTxId 的入参
                    res = aJoinPoint.proceed(args);
                } else {
                    String globalTxId = String.valueOf(args[this.getGlobalTxIdIndex(args, aJoinPoint)]);
                    if (StringUtils.isEmpty(globalTxId)) throw new IllegalArgumentException("子事务方法入参不能找到["+EvalTransactionalConstants.EAVL_GLOBALTX_ID+"]");
                    evalTxManager.beginChildEvalTransactionManager(globalTxId, timeoutSeconds, handler);
                    res = aJoinPoint.proceed(args);
                }
                evalTxManager.commit();
            } catch (Exception e) {
                e.printStackTrace();
                evalTxManager.rollback();
            }
        }
        return res;
    }


    private int getGlobalTxIdIndex(Object[] args, ProceedingJoinPoint aJoinPoint) {
        String[] parameterNames = ((MethodSignature) aJoinPoint.getSignature()).getParameterNames();
        int i = 0;
        for (String pName : parameterNames) {
            if (pName.equals(EvalTransactionalConstants.EAVL_GLOBALTX_ID)) {
                return i;
            }
            i++;
        }
        return i;
    }
}
