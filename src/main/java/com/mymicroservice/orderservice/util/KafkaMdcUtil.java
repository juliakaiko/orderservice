package com.mymicroservice.orderservice.util;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@UtilityClass
public class KafkaMdcUtil {

    /**
     * Adds the traceId from the MDC to the Kafka headers
     */
    public static <T> Message<T> addMdcToMessage(T payload, String key, String topic) {
        MessageBuilder<T> builder = MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, key);

        /**
         *  Take the traceId from MDC and add it to the header.
         */
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isEmpty()) {
            builder.setHeader("X-Trace-Id", traceId);
        }

        /**
         *  Add serviceName to the header.
         */
        String serviceName = MDC.get("serviceName");
        if (serviceName != null && !serviceName.isEmpty()) {
            builder.setHeader("X-Source-Service", serviceName);
        }

        return builder.build();
    }
}
