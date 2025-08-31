package com.mymicroservice.orderservice.configuration;

import com.mymicroservice.orderservice.client.AccessTokenProvider;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserClientConfig {

    /**
     * Feign client interceptor that automatically adds the current JWT access token
     * from the security context to all outgoing requests to UserService.
     * <p>
     * The token is retrieved from current HTTP-request via AccessTokenProvider
     * and added as an Authorization header in "Bearer {token}" format.
     *
     * @param accessTokenProvider provider for accessing the current authentication token
     * @return configured RequestInterceptor instance
     */
    @Bean
    public RequestInterceptor requestInterceptor(AccessTokenProvider accessTokenProvider) {
        return template -> {
            String accessToken = accessTokenProvider.getAccessToken();
            System.out.println("!!!Feign Token: " + accessToken);
            if (accessToken != null && !accessToken.isEmpty()) {
                template.header("Authorization", "Bearer " + accessToken);
            }
        };
    }
}
