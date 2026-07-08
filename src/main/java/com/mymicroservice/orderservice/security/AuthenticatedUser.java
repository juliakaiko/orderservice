package com.mymicroservice.orderservice.security;

import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Authenticated principal built from the JWT forwarded by the Gateway.
 * <p>
 * Carries the resolved {@code userId} and {@code email} directly from token claims,
 * so authorization checks don't need an extra call to the userservice.
 *
 * @param userId numeric user identifier (claim {@code userId})
 * @param email  user email (JWT {@code sub})
 */
public record AuthenticatedUser(Long userId, String email) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return email;
    }
}
