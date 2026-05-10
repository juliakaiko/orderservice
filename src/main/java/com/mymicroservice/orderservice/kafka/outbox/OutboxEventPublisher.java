package com.mymicroservice.orderservice.kafka.outbox;

import com.mymicroservice.orderservice.mapper.JsonMapper;
import com.mymicroservice.orderservice.model.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.messaging.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${kafka.producer.topics.create-order}")
    private String orderTopic;

    public void publish(OutboxEvent event) throws Exception {

        OrderEventDto eventDto = jsonMapper.fromJson(event.getPayload(), OrderEventDto.class);

        Message<OrderEventDto> message = MessageBuilder.withPayload(eventDto)
                    .setHeader(KafkaHeaders.TOPIC, orderTopic)
                    .setHeader(KafkaHeaders.KEY, event.getAggregateId())
                    .setHeader("X-Idempotence-Id", event.getEventId())
                    .setHeader("X-Event-Type", event.getEventType())
                    .setHeader("X-Request-Id", event.getRequestId())
                    .setHeader("X-Source-Service", event.getSourceService())
                    .build();

        kafkaTemplate.send(message).get();

        log.info("Kafka event sent successfully. eventId={}", event.getEventId());
    }
}
