package com.mymicroservice.orderservice.configuration;

import com.mymicroservice.orderservice.client.AccessTokenProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class FeignTestConfig {

    @Bean ("testAccessTokenProvider")
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
