package com.mymicroservice.orderservice.unit.kafka;

import com.mymicroservice.orderservice.kafka.outbox.OutboxEventProducer;
import com.mymicroservice.orderservice.mapper.JsonMapper;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.service.StateService;
import com.mymicroservice.orderservice.util.OrderEventDtoGenerator;
import com.mymicroservice.orderservice.util.OutboxEventGenerator;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.mymicroservice.orderservice.util.data.TestConstants.CREATE_ORDER_TOPIC;
import static com.mymicroservice.orderservice.util.data.TestConstants.ENTITY_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventProducerTest {

    @InjectMocks
    private OutboxEventProducer outboxEventProducer;

    @Mock
    private KafkaTemplate<String, OrderEventDto> kafkaTemplate;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private StateService stateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboxEventProducer, "orderTopic", CREATE_ORDER_TOPIC);
    }

    @Test
    void publishOutboxEvent_ShouldUpdateStatusesToSentAndProcessing_WhenKafkaSendSucceeds() {
        var outboxEvent = OutboxEventGenerator.generateInitialOutboxEvent();
        OrderEventDto orderEventDto = OrderEventDtoGenerator.generateOrderEventDto(ENTITY_ID);
        when(jsonMapper.fromJson(outboxEvent.getPayload(), OrderEventDto.class))
                .thenReturn(Optional.of(orderEventDto));

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(CREATE_ORDER_TOPIC, 0), 0, 0, 0, 0, 0);
        ProducerRecord<String, OrderEventDto> producerRecord =
                new ProducerRecord<>(CREATE_ORDER_TOPIC, orderEventDto);
        SendResult<String, OrderEventDto> sendResult = new SendResult<>(producerRecord, metadata);
        when(kafkaTemplate.send(any(org.springframework.messaging.Message.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        outboxEventProducer.publishOutboxEvent(outboxEvent);

        UUID orderId = UUID.fromString(outboxEvent.getAggregateId());
        org.awaitility.Awaitility.await().untilAsserted(() -> {
            verify(stateService).updateOrderStatus(orderId, OrderStatus.PROCESSING);
            verify(stateService).updateOutboxStatus(outboxEvent.getId(), OutboxEventStatus.SENT);
        });
    }

    @Test
    void publishOutboxEvent_ShouldUpdateOutboxStatusToFailed_WhenKafkaSendFails() {
        var outboxEvent = OutboxEventGenerator.generateInitialOutboxEvent();
        OrderEventDto orderEventDto = OrderEventDtoGenerator.generateOrderEventDto(ENTITY_ID);
        when(jsonMapper.fromJson(outboxEvent.getPayload(), OrderEventDto.class))
                .thenReturn(Optional.of(orderEventDto));

        CompletableFuture<SendResult<String, OrderEventDto>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(any(org.springframework.messaging.Message.class))).thenReturn(failedFuture);

        outboxEventProducer.publishOutboxEvent(outboxEvent);

        org.awaitility.Awaitility.await().untilAsserted(() ->
                verify(stateService).updateOutboxStatus(outboxEvent.getId(), OutboxEventStatus.FAILED));
    }

    @Test
    void publishOutboxEvent_ShouldUpdateOutboxStatusToFailed_WhenDeserializationFails() {
        var outboxEvent = OutboxEventGenerator.generateInitialOutboxEvent();
        when(jsonMapper.fromJson(outboxEvent.getPayload(), OrderEventDto.class)).thenReturn(Optional.empty());

        outboxEventProducer.publishOutboxEvent(outboxEvent);

        verify(stateService).updateOutboxStatus(outboxEvent.getId(), OutboxEventStatus.FAILED);
    }
}
