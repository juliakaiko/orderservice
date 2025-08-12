package com.mymicroservice.orderservice.exception;

import jakarta.persistence.EntityNotFoundException;

public class OrderItemNotFoundException extends EntityNotFoundException {

    public OrderItemNotFoundException(String message) {
        super(message);
    }
}
