package com.mymicroservice.orderservice.unit.advice;

import com.mymicroservice.orderservice.advice.GlobalAdvice;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderAlreadyPaidException;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.util.ErrorItem;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalAdviceTest {

    private final GlobalAdvice globalAdvice = new GlobalAdvice();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void handleItemNotFoundException_ShouldReturnNotFound_WhenItemMissing() {
        setRequestContext("/api/items/1");

        ResponseEntity<ErrorItem> response =
                globalAdvice.handleItemNotFoundException(new ItemNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleOrderAlreadyPaidException_ShouldReturnBadRequest_WhenOrderIsPaid() {
        setRequestContext("/api/orders/1");

        ResponseEntity<ErrorItem> response =
                globalAdvice.handleOrderAlreadyPaidException(new OrderAlreadyPaidException("paid"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleDataIntegrityViolationException_ShouldReturnBadRequest_WhenConstraintViolated() {
        setRequestContext("/api/items");

        ResponseEntity<ErrorItem> response = globalAdvice.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("duplicate key"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleFeignException_ShouldReturnUpstreamStatus_WhenStatusIsKnown() {
        setRequestContext("/api/orders/1");
        FeignException exception = new FeignException(404, "not found") {
        };

        ResponseEntity<ErrorItem> response = globalAdvice.handleFeignException(exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleOrderNotFoundException_ShouldReturnNotFound_WhenOrderMissing() {
        setRequestContext("/api/orders/1");

        ResponseEntity<ErrorItem> response =
                globalAdvice.handleOrderNotFoundException(new OrderNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleAccessDeniedException_ShouldReturnForbidden_WhenAccessDenied() {
        setRequestContext("/api/orders/1");

        ResponseEntity<ErrorItem> response =
                globalAdvice.handleAccessDeniedException(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleFeignException_ShouldReturnBadGateway_WhenStatusIsUnknown() {
        setRequestContext("/api/orders/1");
        FeignException exception = new FeignException(-1, "service unavailable") {
        };

        ResponseEntity<ErrorItem> response = globalAdvice.handleFeignException(exception);

        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getBody().getStatusCode());
    }

    private void setRequestContext(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
