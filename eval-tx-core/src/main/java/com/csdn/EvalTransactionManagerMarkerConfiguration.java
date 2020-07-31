package com.csdn;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvalTransactionManagerMarkerConfiguration {

    @Bean
    public Marker evalTransactionManagerMarkerBean() {
        return new Marker();
    }

    class Marker {
    }
}