package com.mymicroservice.orderservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class OrderAuthorizationService {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    public void verifyOrderOwnership(Long orderUserId) {
        if (isAdmin()) {
            return;
        }
        Long currentUserId = requireCurrentUserId();
        if (!currentUserId.equals(orderUserId)) {
            log.warn("Access denied: user {} attempted to access order of user {}", currentUserId, orderUserId);
            throw new AccessDeniedException("Access denied to order owned by another user");
        }
    }

    public void verifyCanCreateOrderForUser(Long targetUserId) {
        if (isAdmin()) {
            return;
        }
        Long currentUserId = requireCurrentUserId();
        if (!currentUserId.equals(targetUserId)) {
            log.warn("Access denied: user {} attempted to create order for user {}", currentUserId, targetUserId);
            throw new AccessDeniedException("Cannot create order for another user");
        }
    }

    public void verifyCanAccessUserData(Long requestedUserId) {
        if (isAdmin()) {
            return;
        }
        Long currentUserId = requireCurrentUserId();
        if (!currentUserId.equals(requestedUserId)) {
            log.warn("Access denied: user {} attempted to access data of user {}", currentUserId, requestedUserId);
            throw new AccessDeniedException("Access denied to another user's data");
        }
    }

    public Long requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Invalid user id in security context");
        }
    }

    public Optional<Long> getCurrentUserIdIfNotAdmin() {
        if (isAdmin()) {
            return Optional.empty();
        }
        return Optional.of(requireCurrentUserId());
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority(ADMIN_ROLE));
    }
}
