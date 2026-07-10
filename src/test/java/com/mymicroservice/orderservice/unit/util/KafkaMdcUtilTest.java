package com.mymicroservice.orderservice.unit.util;

import com.mymicroservice.orderservice.util.KafkaMdcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KafkaMdcUtilTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void addMdcToMessage_ShouldIncludeTraceAndServiceHeaders_WhenMdcIsSet() {
        MDC.put("traceId", "trace-123");
        MDC.put("serviceName", "orderservice");

        Message<String> message = KafkaMdcUtil.addMdcToMessage("payload", "key-1", "orders-topic");

        assertEquals("payload", message.getPayload());
        assertEquals("orders-topic", message.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals("key-1", message.getHeaders().get(KafkaHeaders.KEY));
        assertEquals("trace-123", message.getHeaders().get("X-Trace-Id"));
        assertEquals("orderservice", message.getHeaders().get("X-Source-Service"));
    }

    @Test
    void addMdcToMessage_ShouldOmitOptionalHeaders_WhenMdcIsEmpty() {
        Message<String> message = KafkaMdcUtil.addMdcToMessage("payload", "key-1", "orders-topic");

        assertNull(message.getHeaders().get("X-Trace-Id"));
        assertNull(message.getHeaders().get("X-Source-Service"));
    }
}
