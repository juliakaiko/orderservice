package com.mymicroservice.orderservice.advice;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderAlreadyPaidException;
import com.mymicroservice.orderservice.exception.OrderItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.util.ErrorItem;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalAdvice {

    /**
     * Handles validation exceptions for DTO fields when data fails validation annotations
     * such as @Valid, @NotNull, @Size, @Pattern and others.
     *
     * @param e MethodArgumentNotValidException containing validation error information
     * @return ResponseEntity with an ErrorItem object containing:
     *         - List of error messages
     *         - URL
     *         - Status code
     *         - Timestamp
     *         - HTTP 400 status (BAD_REQUEST)
     */
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorItem> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ErrorItem error = ErrorItem.fromMethodArgumentNotValid(e,  HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles validation exceptions for controller method parameters,
     * such as @NotEmpty, @NotBlank and others.
     *
     * @param e ConstraintViolationException containing validation error information
     * @return ResponseEntity with an ErrorItem object containing:
     *         - Error message
     *         - URL
     *         - Status code
     *         - Timestamp
     *         - HTTP 400 status (BAD_REQUEST)
     */
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<ErrorItem> handleConstraintViolationException(ConstraintViolationException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles {@link HttpMessageNotReadableException} which occurs when HTTP request body
     * cannot be properly parsed or converted to the expected Java object.
     *
     * <p>This typically happens when:
     * <ul>
     *   <li>Malformed JSON syntax in request body</li>
     *   <li>Type mismatch between JSON values and target Java types</li>
     *   <li>Invalid enum values that cannot be converted to the target enum type</li>
     *   <li>Missing required fields in JSON payload</li>
     * </ul>
     *
     * <p><b>Example error response:</b>
     * <pre>
     * {
     *   "message": "JSON parse error: Cannot coerce empty String (\"\") to `OrderStatus` value",
     *   "timestamp": "2025-08-29 12:12",
     *   "url": "http://localhost:8082/api/orders/7",
     *   "statusCode": 400
     * }
     * </pre>
     *
     * @param e the HttpMessageNotReadableException that was thrown during request processing
     * @return ResponseEntity containing ErrorItem with details about the parsing error
     * @see org.springframework.http.converter.HttpMessageNotReadableException
     * @see org.springframework.http.HttpStatus#BAD_REQUEST
     * @since 1.0
     */
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorItem> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles data integrity violation exceptions, for example,
     * when attempting to save a duplicate unique field (such as name of item).
     *
     * @param e DataIntegrityViolationException containing integrity violation information
     * @return ResponseEntity with an ErrorItem object containing:
     *         - Error message
     *         - URL
     *         - Status code
     *         - Timestamp
     *         - HTTP 400 status (BAD_REQUEST)
     */
    @ExceptionHandler({DataIntegrityViolationException.class})
    public ResponseEntity<ErrorItem> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles exceptions when the Item is not found.
     *
     * @param e ItemNotFoundException
     * @return ResponseEntity with an ErrorItem object containing:
     *         - Error message
     *         - URL
     *         - Status code
     *         - Timestamp
     *         - HTTP 404 status (NOT_FOUND)
     */
    @ExceptionHandler({ItemNotFoundException.class})
    public ResponseEntity<ErrorItem> handleItemNotFoundException(ItemNotFoundException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.NOT_FOUND);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    @ExceptionHandler({OrderItemNotFoundException.class})
    public ResponseEntity<ErrorItem> handleOrderItemNotFoundException(OrderItemNotFoundException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.NOT_FOUND);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    @ExceptionHandler({OrderNotFoundException.class})
    public ResponseEntity<ErrorItem> handleOrderNotFoundException(OrderNotFoundException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.NOT_FOUND);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles Jackson InvalidDefinitionException which occurs during JSON deserialization
     * when there are issues with object mapping definitions, particularly with managed/back references
     * in bidirectional relationships.
     *
     * <p>This exception typically occurs when:
     * <ul>
     *   <li>There are circular references between entities</li>
     *   <li>JsonManagedReference/JsonBackReference annotations are misconfigured</li>
     *   <li>DTO objects contain entity references with bidirectional relationships</li>
     * </ul>
     *
     * @param e the InvalidDefinitionException containing details about the mapping issue
     * @return ResponseEntity with ErrorItem containing details about the deserialization failure
     * @see InvalidDefinitionException
     */
    @ExceptionHandler({InvalidDefinitionException.class})
    public ResponseEntity<ErrorItem> handleInvalidDefinitionException(InvalidDefinitionException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles {@link OrderAlreadyPaidException} thrown when an attempt is made
     * to update an order that is already marked as PAID.
     *
     * @param e the thrown {@link OrderAlreadyPaidException}
     * @return a {@link ResponseEntity} containing an {@link ErrorItem} with
     *         details of the error and HTTP 400 (Bad Request) status
     */
    @ExceptionHandler({OrderAlreadyPaidException.class})
    public ResponseEntity<ErrorItem> handleOrderAlreadyPaidException(OrderAlreadyPaidException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles Feign client exceptions when calling external services.
     * Extracts custom error message from response body if available.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorItem> handleFeignException(FeignException e) {
        HttpStatus status = HttpStatus.valueOf(e.status());
        ErrorItem error = ErrorItem.generateMessage(e, status);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }
}