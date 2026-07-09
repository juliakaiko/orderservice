package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.Set;

import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ITEM_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_ID;

public class OrderDtoGenerator {

    public static OrderDto generateOrderDto() {
        OrderItemDto orderItemDto = OrderItemDto.builder()
                .itemId(TEST_ITEM_ID)
                .quantity(5L)
                .build();

        return OrderDto.builder()
                .userId(TEST_USER_ID)
                .status(OrderStatus.CREATED)
                .creationDate(LocalDateTime.now().withNano(0))
                .orderItems(Set.of(orderItemDto))
                .build();
    }
}
