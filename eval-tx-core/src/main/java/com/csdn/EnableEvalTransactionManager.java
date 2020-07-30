package com.csdn;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({EvalTransactionManagerMarkerConfiguration.class})
public @interface EnableEvalTransactionManager {
}
