package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.enums.OrderStatus;

import java.time.LocalDateTime;

public class OrderGenerator {

    public static Order generateOrder() {
        return  Order.builder()
                .userId(1L)
                .status(OrderStatus.CREATED)
                .creationDate(LocalDateTime.now().withNano(0))
                .build();
    }
}
