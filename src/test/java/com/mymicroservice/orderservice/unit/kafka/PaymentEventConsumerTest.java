package com.mymicroservice.orderservice.unit.kafka;

import com.mymicroservice.orderservice.kafka.PaymentEventConsumer;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.service.StateService;
import com.mymicroservice.orderservice.util.PaymentEventDtoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.UUID;

import static com.mymicroservice.orderservice.util.data.TestConstants.CREATE_ORDER_EVENT_TYPE;
import static com.mymicroservice.orderservice.util.data.TestConstants.IDEMPOTENCE_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.SERVICE_NAME;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ORDER_UUID;
import static com.mymicroservice.orderservice.util.data.TestConstants.TRACE_ID;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Mock
    private StateService stateService;

    @Mock
    private Acknowledgment acknowledgment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentEventConsumer, "serviceName", "orderservice");
    }

    @Test
    void onCreatePayment_ShouldUpdateOrderStatusToPaid_WhenStatusIsPaid() {
        PaymentEventDto event = PaymentEventDtoGenerator.generatePaidPaymentEventDto(TEST_ORDER_UUID.toString());

        paymentEventConsumer.onCreatePayment(
                event, "key", 0, 0L,
                IDEMPOTENCE_ID, CREATE_ORDER_EVENT_TYPE, TRACE_ID, SERVICE_NAME,
                acknowledgment
        );

        verify(stateService).updateOrderStatus(TEST_ORDER_UUID, OrderStatus.PAID);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onCreatePayment_ShouldUpdateOrderStatusToFailed_WhenStatusIsFailed() {
        PaymentEventDto event = PaymentEventDtoGenerator.generateFailedPaymentEventDto(TEST_ORDER_UUID.toString());

        paymentEventConsumer.onCreatePayment(
                event, "key", 0, 0L,
                IDEMPOTENCE_ID, CREATE_ORDER_EVENT_TYPE, TRACE_ID, SERVICE_NAME,
                acknowledgment
        );

        verify(stateService).updateOrderStatus(TEST_ORDER_UUID, OrderStatus.FAILED);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onCreatePayment_ShouldNotAcknowledge_WhenEventIsNull() {
        paymentEventConsumer.onCreatePayment(
                null, "key", 0, 0L,
                IDEMPOTENCE_ID, CREATE_ORDER_EVENT_TYPE, TRACE_ID, SERVICE_NAME,
                acknowledgment
        );

        verify(stateService, never()).updateOrderStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void onCreatePayment_ShouldAcknowledge_WhenOrderIdIsInvalid() {
        PaymentEventDto event = PaymentEventDtoGenerator.generatePaidPaymentEventDto("invalid-uuid");

        paymentEventConsumer.onCreatePayment(
                event, "key", 0, 0L,
                IDEMPOTENCE_ID, CREATE_ORDER_EVENT_TYPE, TRACE_ID, SERVICE_NAME,
                acknowledgment
        );

        verify(stateService, never()).updateOrderStatus(org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.any());
        verify(acknowledgment).acknowledge();
        verify(acknowledgment, never()).nack(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onCreatePayment_ShouldNack_WhenStateUpdateFails() {
        PaymentEventDto event = PaymentEventDtoGenerator.generatePaidPaymentEventDto(TEST_ORDER_UUID.toString());
        org.mockito.Mockito.doThrow(new RuntimeException("db error"))
                .when(stateService).updateOrderStatus(TEST_ORDER_UUID, OrderStatus.PAID);

        paymentEventConsumer.onCreatePayment(
                event, "key", 0, 0L,
                IDEMPOTENCE_ID, CREATE_ORDER_EVENT_TYPE, TRACE_ID, SERVICE_NAME,
                acknowledgment
        );

        verify(acknowledgment).nack(Duration.ofMillis(100));
    }
}
