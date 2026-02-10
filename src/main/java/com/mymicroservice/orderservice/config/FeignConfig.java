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

            String requestId = MDC.get("requestId");
            if (requestId != null && !requestId.isEmpty()) {
                requestTemplate.header("X-Request-Id", requestId);
            }
        };
    }
}
