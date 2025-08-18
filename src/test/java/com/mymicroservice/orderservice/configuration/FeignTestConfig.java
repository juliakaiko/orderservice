package com.mymicroservice.orderservice.configuration;

import com.mymicroservice.orderservice.security.AccessTokenProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import feign.RequestInterceptor;

@TestConfiguration
public class FeignTestConfig {

    @Bean
    @Primary
    public AccessTokenProvider accessTokenProvider() {
        return new AccessTokenProvider() {
            @Override
            public String getAccessToken() {
                return "test-token";
            }
        };
    }

    @Bean
    public RequestInterceptor authRequestInterceptor(AccessTokenProvider accessTokenProvider) {
        return template -> {
            String token = accessTokenProvider.getAccessToken();
            template.header("Authorization", "Bearer " + token);
        };
    }
}
