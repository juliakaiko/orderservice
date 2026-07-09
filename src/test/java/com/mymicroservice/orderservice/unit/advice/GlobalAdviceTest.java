package com.mymicroservice.orderservice.unit.advice;

import com.mymicroservice.orderservice.advice.GlobalAdvice;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderAlreadyPaidException;
import com.mymicroservice.orderservice.exception.OrderItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.exception.OutboxEventNotFoundException;
import com.mymicroservice.orderservice.util.ErrorItem;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

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

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequest_WhenValidationFails() {
        setRequestContext("/api/orders");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "orderDto");
        bindingResult.addError(new FieldError("orderDto", "userId", "must not be null"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorItem> response = globalAdvice.handleMethodArgumentNotValidException(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleConstraintViolationException_ShouldReturnBadRequest_WhenConstraintViolated() {
        setRequestContext("/api/orders/find-by-ids");
        ConstraintViolationException exception = mock(ConstraintViolationException.class);

        ResponseEntity<ErrorItem> response = globalAdvice.handleConstraintViolationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleHttpMessageNotReadableException_ShouldReturnBadRequest_WhenBodyInvalid() {
        setRequestContext("/api/orders");

        ResponseEntity<ErrorItem> response = globalAdvice.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("invalid json"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleOrderItemNotFoundException_ShouldReturnNotFound_WhenOrderItemMissing() {
        setRequestContext("/api/order-items/1");

        ResponseEntity<ErrorItem> response = globalAdvice.handleOrderItemNotFoundException(
                new OrderItemNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleOutboxEventNotFoundException_ShouldReturnNotFound_WhenOutboxEventMissing() {
        setRequestContext("/api/outbox/1");

        ResponseEntity<ErrorItem> response = globalAdvice.handleOutboxEventNotFoundException(
                new OutboxEventNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleInvalidDefinitionException_ShouldReturnBadRequest_WhenMappingInvalid() {
        setRequestContext("/api/orders");
        InvalidDefinitionException exception = mock(InvalidDefinitionException.class);

        ResponseEntity<ErrorItem> response = globalAdvice.handleInvalidDefinitionException(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    private void setRequestContext(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
