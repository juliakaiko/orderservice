package com.mymicroservice.orderservice.exception;

import jakarta.persistence.EntityNotFoundException;

public class OutboxEventNotFoundException extends EntityNotFoundException {

    public OutboxEventNotFoundException(String message) {
        super(message);
    }
}
