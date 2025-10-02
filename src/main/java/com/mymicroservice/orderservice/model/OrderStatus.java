package com.mymicroservice.orderservice.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OrderStatus {

    CREATED ("CREATED"),
    PROCESSING ("PROCESSING"),
    PAID ("PAID"),
    CANCELLED ("CANCELLED"),
    FAILED ("FAILED");

    private final String status;
}
