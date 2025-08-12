package com.mymicroservice.orderservice.exception;

import jakarta.persistence.EntityNotFoundException;

public class ItemNotFoundException extends EntityNotFoundException {

    public ItemNotFoundException(String message) {
        super(message);
    }
}
