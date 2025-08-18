package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.model.OrderItem;

public class OrderItemGenerator {

    public static OrderItem generateOrderItem() {

        return  OrderItem.builder()
                .quantity(10l)
                .build();
    }
}
