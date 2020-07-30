package com.csdn;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvalTransactionManagerMarkerConfiguration {
    public EvalTransactionManagerMarkerConfiguration() {
    }

    @Bean
    public EvalTransactionManagerMarkerConfiguration.Marker evalTransactionManagerMarkerBean() {
        return new EvalTransactionManagerMarkerConfiguration.Marker();
    }

   public class Marker {
        Marker() {
        }
    }
}