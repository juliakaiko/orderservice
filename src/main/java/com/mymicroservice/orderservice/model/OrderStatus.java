package com.mymicroservice.orderservice.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OrderStatus {
    NEW ("NEW"),
    DELIVERED ("DELIVERED"),
    CANCELLED ("CANCELLED"),
    SHIPPED ("SHIPPED"),
    PROCESSING ("PROCESSING");

    private final String status;
}
