package com.mymicroservice.orderservice.unit.service;

import com.mymicroservice.orderservice.kafka.outbox.OutboxEventProducer;
import com.mymicroservice.orderservice.mapper.JsonMapper;
import com.mymicroservice.orderservice.model.OutboxEvent;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.repository.OutboxEventRepository;
import com.mymicroservice.orderservice.service.StateService;
import com.mymicroservice.orderservice.service.impl.OutboxServiceImpl;
import com.mymicroservice.orderservice.util.OrderEventDtoGenerator;
import com.mymicroservice.orderservice.util.OutboxEventGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mymicroservice.orderservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.ORDER_CREATED_EVENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceImplTest {

    @InjectMocks
    private OutboxServiceImpl outboxService;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private OutboxEventProducer kafkaEventPublisher;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private StateService stateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboxService, "processingTimeoutMinutes", 5);
        ReflectionTestUtils.setField(outboxService, "batchSize", 100);
    }

    @Test
    void saveOutboxEvent_ShouldPersistEvent_WhenSerializationSucceeds() {
        OrderEventDto orderEventDto = OrderEventDtoGenerator.generateOrderEventDto(ENTITY_ID);
        when(jsonMapper.toJson(orderEventDto)).thenReturn(Optional.of("{\"orderId\":\"1\"}"));

        outboxService.saveOutboxEvent(orderEventDto, ORDER_CREATED_EVENT_TYPE);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertEquals(ENTITY_ID, captor.getValue().getAggregateId());
        assertEquals(ORDER_CREATED_EVENT_TYPE, captor.getValue().getEventType());
        assertEquals(OutboxEventStatus.INITIAL, captor.getValue().getStatus());
    }

    @Test
    void saveOutboxEvent_ShouldThrowIllegalStateException_WhenSerializationFails() {
        OrderEventDto orderEventDto = OrderEventDtoGenerator.generateOrderEventDto();
        when(jsonMapper.toJson(orderEventDto)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> outboxService.saveOutboxEvent(orderEventDto, ORDER_CREATED_EVENT_TYPE));

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void processPendingOutboxEvents_ShouldPublishEvents_WhenPendingEventsExist() {
        OutboxEvent event = OutboxEventGenerator.generateInitialOutboxEvent();
        when(outboxEventRepository.resetStaleProcessingEvents(any(LocalDateTime.class))).thenReturn(0);
        when(outboxEventRepository.findEventsForProcessing(
                List.of(OutboxEventStatus.INITIAL.name(), OutboxEventStatus.FAILED.name()), 100))
                .thenReturn(List.of(event));

        outboxService.processPendingOutboxEvents();

        verify(outboxEventRepository).resetStaleProcessingEvents(any(LocalDateTime.class));
        verify(stateService).updateOutboxStatus(event.getId(), OutboxEventStatus.PROCESSING);
        verify(kafkaEventPublisher).publishOutboxEvent(event);
    }

    @Test
    void processPendingOutboxEvents_ShouldMarkEventAsFailed_WhenProcessingThrowsException() {
        OutboxEvent event = OutboxEventGenerator.generateInitialOutboxEvent();
        when(outboxEventRepository.resetStaleProcessingEvents(any(LocalDateTime.class))).thenReturn(0);
        when(outboxEventRepository.findEventsForProcessing(any(), anyInt())).thenReturn(List.of(event));
        doThrow(new RuntimeException("publish failed"))
                .when(stateService).updateOutboxStatus(event.getId(), OutboxEventStatus.PROCESSING);

        outboxService.processPendingOutboxEvents();

        verify(stateService).updateOutboxStatus(event.getId(), OutboxEventStatus.FAILED);
        verify(kafkaEventPublisher, never()).publishOutboxEvent(event);
    }

    @Test
    void processPendingOutboxEvents_ShouldContinueProcessing_WhenSingleEventFails() {
        OutboxEvent first = OutboxEventGenerator.generateInitialOutboxEvent();
        OutboxEvent second = OutboxEventGenerator.generateOutboxEvent(
                UUID.randomUUID(), ENTITY_ID, OutboxEventStatus.INITIAL);
        when(outboxEventRepository.resetStaleProcessingEvents(any(LocalDateTime.class))).thenReturn(1);
        when(outboxEventRepository.findEventsForProcessing(any(), anyInt()))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("publish failed"))
                .when(stateService).updateOutboxStatus(eq(first.getId()), eq(OutboxEventStatus.PROCESSING));

        outboxService.processPendingOutboxEvents();

        verify(kafkaEventPublisher).publishOutboxEvent(second);
        verify(stateService).updateOutboxStatus(first.getId(), OutboxEventStatus.FAILED);
    }
}
