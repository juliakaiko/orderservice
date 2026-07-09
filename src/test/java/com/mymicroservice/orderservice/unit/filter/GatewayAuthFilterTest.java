package com.mymicroservice.orderservice.unit.filter;

import com.mymicroservice.orderservice.filter.GatewayAuthFilter;
import com.mymicroservice.orderservice.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.mymicroservice.orderservice.util.CommonConstants.GATEWAY_SERVICE_NAME;
import static com.mymicroservice.orderservice.util.CommonConstants.INTERNAL_CALL_HEADER;
import static com.mymicroservice.orderservice.util.CommonConstants.SOURCE_SERVICE_HEADER;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_EMAIL;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayAuthFilterTest {

    @InjectMocks
    private GatewayAuthFilter gatewayAuthFilter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldAuthenticateUser_WhenGatewayHeadersAndJwtAreValid() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, GATEWAY_SERVICE_NAME);
        request.addHeader("Authorization", "Bearer " + buildJwt(TEST_USER_ID, TEST_USER_EMAIL, "USER"));

        gatewayAuthFilter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        assertEquals(TEST_USER_ID, principal.userId());
        assertEquals(TEST_USER_EMAIL, principal.email());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearSecurityContext_WhenUserIdClaimMissing() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, GATEWAY_SERVICE_NAME);
        request.addHeader("Authorization", "Bearer " + buildJwtWithoutUserId(TEST_USER_EMAIL, "USER"));

        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearSecurityContext_WhenInternalServiceCallDetected() throws Exception {
        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearSecurityContext_WhenBearerTokenIsMissing() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, GATEWAY_SERVICE_NAME);

        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearSecurityContext_WhenJwtStructureIsInvalid() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, GATEWAY_SERVICE_NAME);
        request.addHeader("Authorization", "Bearer invalid-token");

        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldAuthenticateAdmin_WhenAdminRolePresent() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, GATEWAY_SERVICE_NAME);
        request.addHeader("Authorization", "Bearer " + buildJwt(TEST_USER_ID, TEST_USER_EMAIL, "ADMIN"));

        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    private String buildJwt(Long userId, String email, String role) {
        String payloadJson = "{\"sub\":\"" + email + "\",\"userId\":" + userId
                + ",\"roles\":[\"" + role + "\"]}";
        return encodeJwt(payloadJson);
    }

    private String buildJwtWithoutUserId(String email, String role) {
        String payloadJson = "{\"sub\":\"" + email + "\",\"roles\":[\"" + role + "\"]}";
        return encodeJwt(payloadJson);
    }

    private String encodeJwt(String payloadJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }
}
