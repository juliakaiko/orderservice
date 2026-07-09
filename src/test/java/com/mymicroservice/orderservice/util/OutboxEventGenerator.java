package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.model.OutboxEvent;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.mymicroservice.orderservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.ORDER_CREATED_EVENT_TYPE;
import static com.mymicroservice.orderservice.util.data.TestConstants.SOURCE_SERVICE;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ORDER_UUID;
import static com.mymicroservice.orderservice.util.data.TestConstants.TRACE_ID;

public class OutboxEventGenerator {

    public static OutboxEvent generateInitialOutboxEvent() {
        return generateOutboxEvent(UUID.randomUUID(), TEST_ORDER_UUID.toString(), OutboxEventStatus.INITIAL);
    }

    public static OutboxEvent generateOutboxEvent(UUID id, String aggregateId, OutboxEventStatus status) {
        return OutboxEvent.builder()
                .id(id)
                .aggregateId(aggregateId)
                .eventType(ORDER_CREATED_EVENT_TYPE)
                .payload("{\"orderId\":\"" + aggregateId + "\",\"userId\":\"" + ENTITY_ID
                        + "\",\"paymentAmount\":500}")
                .status(status)
                .traceId(TRACE_ID)
                .sourceService(SOURCE_SERVICE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
