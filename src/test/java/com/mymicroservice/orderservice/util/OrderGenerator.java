package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.enums.OrderStatus;

import java.time.LocalDateTime;

import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_ID;

public class OrderGenerator {

    public static Order generateOrder() {
        return Order.builder()
                .userId(TEST_USER_ID)
                .status(OrderStatus.CREATED)
                .creationDate(LocalDateTime.now().withNano(0))
                .build();
    }
}
