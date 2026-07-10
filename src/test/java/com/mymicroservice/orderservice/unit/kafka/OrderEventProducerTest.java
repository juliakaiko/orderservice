package com.mymicroservice.orderservice.unit.kafka;

import com.mymicroservice.orderservice.kafka.OrderEventProducer;
import com.mymicroservice.orderservice.util.OrderEventDtoGenerator;
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

import java.util.concurrent.CompletableFuture;

import static com.mymicroservice.orderservice.util.data.TestConstants.CREATE_ORDER_TOPIC;
import static com.mymicroservice.orderservice.util.data.TestConstants.ENTITY_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @InjectMocks
    private OrderEventProducer orderEventProducer;

    @Mock
    private KafkaTemplate<String, OrderEventDto> kafkaTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderEventProducer, "orderTopic", CREATE_ORDER_TOPIC);
    }

    @Test
    void sendCreateOrder_ShouldInvokeSuccessCallback_WhenKafkaSendSucceeds() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto(ENTITY_ID);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(CREATE_ORDER_TOPIC, 0), 0, 0, 0, 0, 0);
        ProducerRecord<String, OrderEventDto> producerRecord =
                new ProducerRecord<>(CREATE_ORDER_TOPIC, event);
        SendResult<String, OrderEventDto> sendResult = new SendResult<>(producerRecord, metadata);
        when(kafkaTemplate.send(any(org.springframework.messaging.Message.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        Runnable onSuccess = org.mockito.Mockito.mock(Runnable.class);

        orderEventProducer.sendCreateOrder(event, onSuccess);

        org.awaitility.Awaitility.await().untilAsserted(() -> verify(onSuccess).run());
    }

    @Test
    void sendCreateOrder_ShouldLogError_WhenKafkaSendFails() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto(ENTITY_ID);
        CompletableFuture<SendResult<String, OrderEventDto>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(any(org.springframework.messaging.Message.class))).thenReturn(failedFuture);
        Runnable onSuccess = org.mockito.Mockito.mock(Runnable.class);

        orderEventProducer.sendCreateOrder(event, onSuccess);

        org.awaitility.Awaitility.await().pollDelay(java.time.Duration.ofMillis(100)).untilAsserted(() ->
                org.mockito.Mockito.verifyNoInteractions(onSuccess));
    }
}
