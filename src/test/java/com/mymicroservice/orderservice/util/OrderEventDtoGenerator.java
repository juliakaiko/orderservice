package com.mymicroservice.orderservice.util;

import org.mymicroservices.common.events.OrderEventDto;

import java.math.BigDecimal;

import static com.mymicroservice.orderservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.SECOND_ENTITY_ID;

public class OrderEventDtoGenerator {

    public static OrderEventDto generateOrderEventDto() {
        return generateOrderEventDto(ENTITY_ID, ENTITY_ID, BigDecimal.valueOf(500));
    }

    public static OrderEventDto generateOrderEventDto(String orderId) {
        return generateOrderEventDto(orderId, ENTITY_ID, BigDecimal.valueOf(500));
    }

    public static OrderEventDto generateOrderEventDto(String orderId, String userId, BigDecimal amount) {
        return OrderEventDto.builder()
                .orderId(orderId)
                .userId(userId)
                .paymentAmount(amount)
                .build();
    }

    public static OrderEventDto generateSecondOrderEventDto() {
        return generateOrderEventDto(SECOND_ENTITY_ID, ENTITY_ID, BigDecimal.valueOf(1000));
    }
}
