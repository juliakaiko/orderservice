package com.mymicroservice.orderservice.util;

import org.mymicroservices.common.events.PaymentEventDto;

import java.math.BigDecimal;

import static com.mymicroservice.orderservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.FAILED_STATUS;
import static com.mymicroservice.orderservice.util.data.TestConstants.PAID_STATUS;
import static com.mymicroservice.orderservice.util.data.TestConstants.PAYMENT_ID;

public class PaymentEventDtoGenerator {

    public static PaymentEventDto generatePaidPaymentEventDto() {
        return generatePaymentEventDto(PAYMENT_ID, ENTITY_ID, ENTITY_ID, PAID_STATUS, BigDecimal.valueOf(500));
    }

    public static PaymentEventDto generatePaidPaymentEventDto(String orderId) {
        return generatePaymentEventDto(PAYMENT_ID, orderId, ENTITY_ID, PAID_STATUS, BigDecimal.valueOf(500));
    }

    public static PaymentEventDto generateFailedPaymentEventDto(String orderId) {
        return generatePaymentEventDto(PAYMENT_ID, orderId, ENTITY_ID, FAILED_STATUS, BigDecimal.valueOf(500));
    }

    public static PaymentEventDto generatePaymentEventDto(
            String paymentId,
            String orderId,
            String userId,
            String status,
            BigDecimal amount
    ) {
        return PaymentEventDto.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .status(status)
                .paymentAmount(amount)
                .build();
    }
}
