package com.mymicroservice.orderservice.util;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@UtilityClass
public class KafkaMdcUtil {

    /**
     * Adds the requestId from the MDC to the Kafka headers
     */
    public static <T> Message<T> addMdcToMessage(T payload, String key, String topic) {
        MessageBuilder<T> builder = MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, key);

        /**
         *  Take the requestId from MDC and add it to the header.
         */
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isEmpty()) {
            builder.setHeader("X-Request-Id", requestId);
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
