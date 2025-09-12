package com.mymicroservice.orderservice.exception;

public class OrderAlreadyPaidException extends RuntimeException {
    public OrderAlreadyPaidException(String message) {
        super(message);
    }
}
