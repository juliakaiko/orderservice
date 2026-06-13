package com.mymicroservice.orderservice.unit.util;

import com.mymicroservice.orderservice.model.OutboxEvent;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.util.MdcUtils;
import com.mymicroservice.orderservice.util.OutboxEventGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static com.mymicroservice.orderservice.util.data.TestConstants.SOURCE_SERVICE;
import static com.mymicroservice.orderservice.util.data.TestConstants.TRACE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MdcUtilsTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void runWithOutboxEvent_ShouldSetAndRestoreMdc_WhenEventHasTraceData() {
        MDC.put("traceId", "old-trace");
        MDC.put("serviceName", "old-service");

        OutboxEvent event = OutboxEventGenerator.generateOutboxEvent(
                java.util.UUID.randomUUID(), "1", OutboxEventStatus.INITIAL);

        MdcUtils.runWithOutboxEvent(event, () -> {
            assertEquals(TRACE_ID, MDC.get("traceId"));
            assertEquals(SOURCE_SERVICE, MDC.get("serviceName"));
        });

        assertEquals("old-trace", MDC.get("traceId"));
        assertEquals("old-service", MDC.get("serviceName"));
    }

    @Test
    void runWithOutboxEvent_ShouldRemoveMdcKeys_WhenPreviousValuesWereAbsent() {
        OutboxEvent event = OutboxEventGenerator.generateOutboxEvent(
                java.util.UUID.randomUUID(), "1", OutboxEventStatus.INITIAL);

        MdcUtils.runWithOutboxEvent(event, () -> {
            assertEquals(TRACE_ID, MDC.get("traceId"));
        });

        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("serviceName"));
    }
}
