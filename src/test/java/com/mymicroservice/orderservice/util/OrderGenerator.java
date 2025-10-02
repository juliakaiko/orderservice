package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderStatus;

import java.time.LocalDate;

public class OrderGenerator {

    public static Order generateOrder() {
        return  Order.builder()
                .userId(1l)
                .status(OrderStatus.CREATED)
                .creationDate(LocalDate.now())
                .build();
    }
}
