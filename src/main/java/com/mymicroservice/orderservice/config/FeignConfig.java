package com.mymicroservice.orderservice.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalCallInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Internal-Call", "true");

            String traceId = MDC.get("traceId");
            if (traceId != null && !traceId.isEmpty()) {
                requestTemplate.header("X-Trace-Id", traceId);
            }
        };
    }
}
