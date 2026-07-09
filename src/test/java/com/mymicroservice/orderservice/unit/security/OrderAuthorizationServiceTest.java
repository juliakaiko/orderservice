package com.mymicroservice.orderservice.unit.security;

import com.mymicroservice.orderservice.security.AuthenticatedUser;
import com.mymicroservice.orderservice.security.OrderAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_EMAIL;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class OrderAuthorizationServiceTest {

    @InjectMocks
    private OrderAuthorizationService orderAuthorizationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void verifyOrderOwnership_ShouldAllowAccess_WhenUserOwnsOrder() {
        authenticateAsUser(TEST_USER_ID);

        assertDoesNotThrow(() -> orderAuthorizationService.verifyOrderOwnership(TEST_USER_ID));
    }

    @Test
    void verifyOrderOwnership_ShouldThrowAccessDeniedException_WhenUserDoesNotOwnOrder() {
        authenticateAsUser(TEST_USER_ID);

        assertThrows(AccessDeniedException.class,
                () -> orderAuthorizationService.verifyOrderOwnership(999L));
    }

    @Test
    void verifyOrderOwnership_ShouldAllowAccess_WhenUserIsAdmin() {
        authenticateAsAdmin();

        assertDoesNotThrow(() -> orderAuthorizationService.verifyOrderOwnership(999L));
    }

    @Test
    void verifyCanCreateOrderForUser_ShouldThrowAccessDeniedException_WhenCreatingForAnotherUser() {
        authenticateAsUser(TEST_USER_ID);

        assertThrows(AccessDeniedException.class,
                () -> orderAuthorizationService.verifyCanCreateOrderForUser(999L));
    }

    @Test
    void requireCurrentUserId_ShouldReturnUserId_WhenAuthenticated() {
        authenticateAsUser(TEST_USER_ID);

        assertEquals(TEST_USER_ID, orderAuthorizationService.requireCurrentUserId());
    }

    @Test
    void requireCurrentUserEmail_ShouldReturnEmail_WhenAuthenticated() {
        authenticateAsUser(TEST_USER_ID);

        assertEquals(TEST_USER_EMAIL, orderAuthorizationService.requireCurrentUserEmail());
    }

    @Test
    void isAdmin_ShouldReturnTrue_WhenAdminRolePresent() {
        authenticateAsAdmin();

        assertTrue(orderAuthorizationService.isAdmin());
    }

    @Test
    void verifyCanAccessUserData_ShouldThrowAccessDeniedException_WhenAccessingAnotherUser() {
        authenticateAsUser(TEST_USER_ID);

        assertThrows(AccessDeniedException.class,
                () -> orderAuthorizationService.verifyCanAccessUserData(999L));
    }

    @Test
    void verifyCanAccessUserData_ShouldAllowAccess_WhenUserRequestsOwnData() {
        authenticateAsUser(TEST_USER_ID);

        assertDoesNotThrow(() -> orderAuthorizationService.verifyCanAccessUserData(TEST_USER_ID));
    }

    @Test
    void verifyCanAccessUserEmail_ShouldAllowAccess_WhenUserRequestsOwnEmail() {
        authenticateAsUser(TEST_USER_ID);

        assertDoesNotThrow(() -> orderAuthorizationService.verifyCanAccessUserEmail(TEST_USER_EMAIL));
    }

    @Test
    void verifyCanAccessUserEmail_ShouldThrowAccessDeniedException_WhenAccessingAnotherUser() {
        authenticateAsUser(TEST_USER_ID);

        assertThrows(AccessDeniedException.class,
                () -> orderAuthorizationService.verifyCanAccessUserEmail("other@example.com"));
    }

    @Test
    void getCurrentUserIdIfNotAdmin_ShouldReturnEmpty_WhenUserIsAdmin() {
        authenticateAsAdmin();

        assertTrue(orderAuthorizationService.getCurrentUserIdIfNotAdmin().isEmpty());
    }

    @Test
    void getCurrentUserIdIfNotAdmin_ShouldReturnUserId_WhenUserIsNotAdmin() {
        authenticateAsUser(TEST_USER_ID);

        assertEquals(TEST_USER_ID, orderAuthorizationService.getCurrentUserIdIfNotAdmin().orElseThrow());
    }

    @Test
    void requireCurrentUserId_ShouldThrowAccessDeniedException_WhenNotAuthenticated() {
        assertThrows(AccessDeniedException.class, () -> orderAuthorizationService.requireCurrentUserId());
    }

    @Test
    void verifyCanCreateOrderForUser_ShouldAllowAccess_WhenCreatingForSelf() {
        authenticateAsUser(TEST_USER_ID);

        assertDoesNotThrow(() -> orderAuthorizationService.verifyCanCreateOrderForUser(TEST_USER_ID));
    }

    private void authenticateAsUser(Long userId) {
        setPrincipal(new AuthenticatedUser(userId, TEST_USER_EMAIL), "ROLE_USER");
    }

    private void authenticateAsAdmin() {
        setPrincipal(new AuthenticatedUser(TEST_USER_ID, TEST_USER_EMAIL), "ROLE_ADMIN");
    }

    private void setPrincipal(AuthenticatedUser principal, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                )
        );
    }
}
