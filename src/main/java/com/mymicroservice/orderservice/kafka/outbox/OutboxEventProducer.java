package com.mymicroservice.orderservice.kafka.outbox;

import com.mymicroservice.orderservice.mapper.JsonMapper;
import com.mymicroservice.orderservice.model.OutboxEvent;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.service.StateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProducer {

    private final KafkaTemplate<String, OrderEventDto> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final StateService stateService;

    @Value("${kafka.producer.topics.create-order}")
    private String orderTopic;

    public void publishOutboxEvent(OutboxEvent event) {
        jsonMapper.fromJson(event.getPayload(), OrderEventDto.class)
                .ifPresentOrElse(eventDto -> {
                    Message<OrderEventDto> message =
                            MessageBuilder.withPayload(eventDto)
                                    .setHeader(KafkaHeaders.TOPIC, orderTopic)
                                    .setHeader(KafkaHeaders.KEY, event.getAggregateId())
                                    .setHeader("X-Idempotence-Id", event.getId())
                                    .setHeader("X-Event-Type", event.getEventType())
                                    .setHeader("X-Trace-Id", event.getTraceId())
                                    .setHeader("X-Source-Service", event.getSourceService())
                                    .build();

                    CompletableFuture<SendResult<String, OrderEventDto>> future = kafkaTemplate.send(message);

                    log.info("Kafka send initiated. eventId={}", event.getId());
                //сработает только после того, как Kafka Producer закончит попытку отправки сообщения
                    future.whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("CREATE_ORDER sent successfully to Kafka, orderId={}, offset={}",
                                    event.getAggregateId(),
                                    result.getRecordMetadata().offset()
                            );
                            try {
                                stateService.updateOrderStatus(UUID.fromString(event.getAggregateId()),
                                        OrderStatus.PROCESSING
                                );
                            } catch (Exception e) {
                                log.error("Failed to update status to PROCESSING for orderId={}", event.getAggregateId());
                            }
                            try {
                                stateService.updateOutboxStatus(event.getId(), OutboxEventStatus.SENT);
                            } catch (Exception e) {
                                log.error("Failed to update status to SEND for eventId={}", event.getId());
                            }
                        } else {
                            log.error("Failed to send CREATE_ORDER to Kafka, orderId={}, exception={}", event.getAggregateId(),
                                    ex.getClass().getSimpleName());
                            try {
                                stateService.updateOutboxStatus(event.getId(), OutboxEventStatus.FAILED);
                            } catch (Exception e) {
                                log.error("Failed to update status to FAILED for eventId={}", event.getId());
                            }
                        }
                    });
                }, () -> {
                    log.error("Failed to deserialize OrderEventDto from JSON. eventId={}", event.getId());
                    stateService.updateOutboxStatus(event.getId(), OutboxEventStatus.FAILED);
                });
    }
}
