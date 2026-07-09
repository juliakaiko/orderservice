package com.mymicroservice.orderservice.unit.service;

import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.exception.OutboxEventNotFoundException;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.model.OutboxEvent;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.repository.OutboxEventRepository;
import com.mymicroservice.orderservice.service.impl.StateServiceImpl;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.OutboxEventGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ITEM_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ORDER_UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateServiceImplTest {

    @InjectMocks
    private StateServiceImpl stateService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private Order testOrder;
    private OutboxEvent testOutboxEvent;
    private final UUID testOutboxEventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testOrder = OrderGenerator.generateOrder();
        testOrder.setId(TEST_ORDER_UUID);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(5L);

        Item item = new Item();
        item.setId(TEST_ITEM_ID);
        item.setPrice(BigDecimal.valueOf(100));
        orderItem.setItem(item);
        orderItem.setOrder(testOrder);

        testOrder.setOrderItems(Set.of(orderItem));
        testOutboxEvent = OutboxEventGenerator.generateOutboxEvent(
                testOutboxEventId, TEST_ORDER_UUID.toString(), OutboxEventStatus.INITIAL);
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatus_WhenOrderExists() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));

        stateService.updateOrderStatus(TEST_ORDER_UUID, OrderStatus.PROCESSING);

        assertEquals(OrderStatus.PROCESSING, testOrder.getStatus());
        verify(orderRepository).findById(TEST_ORDER_UUID);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrderStatus_ShouldThrowOrderNotFoundException_WhenOrderNotFound() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> stateService.updateOrderStatus(TEST_ORDER_UUID, OrderStatus.PROCESSING));

        verify(orderRepository, times(1)).findById(TEST_ORDER_UUID);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_ShouldThrowIllegalArgumentException_WhenOrderIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> stateService.updateOrderStatus(null, OrderStatus.PROCESSING));

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOutboxStatus_ShouldUpdateStatusToProcessing_WhenOutboxEventExists() {
        when(outboxEventRepository.findById(testOutboxEventId)).thenReturn(Optional.of(testOutboxEvent));

        stateService.updateOutboxStatus(testOutboxEventId, OutboxEventStatus.PROCESSING);

        assertEquals(OutboxEventStatus.PROCESSING, testOutboxEvent.getStatus());
        assertNotNull(testOutboxEvent.getProcessedAt());
        assertNotNull(testOutboxEvent.getUpdatedAt());
        verify(outboxEventRepository, times(1)).save(testOutboxEvent);
    }

    @Test
    void updateOutboxStatus_ShouldUpdateStatusToSent_WhenOutboxEventExists() {
        when(outboxEventRepository.findById(testOutboxEventId)).thenReturn(Optional.of(testOutboxEvent));

        stateService.updateOutboxStatus(testOutboxEventId, OutboxEventStatus.SENT);

        assertEquals(OutboxEventStatus.SENT, testOutboxEvent.getStatus());
        verify(outboxEventRepository, times(1)).save(testOutboxEvent);
    }

    @Test
    void updateOutboxStatus_ShouldUpdateStatusToFailed_WhenOutboxEventExists() {
        when(outboxEventRepository.findById(testOutboxEventId)).thenReturn(Optional.of(testOutboxEvent));

        stateService.updateOutboxStatus(testOutboxEventId, OutboxEventStatus.FAILED);

        assertEquals(OutboxEventStatus.FAILED, testOutboxEvent.getStatus());
        verify(outboxEventRepository, times(1)).save(testOutboxEvent);
    }

    @Test
    void updateOutboxStatus_ShouldThrowOutboxEventNotFoundException_WhenOutboxEventNotFound() {
        when(outboxEventRepository.findById(testOutboxEventId)).thenReturn(Optional.empty());

        assertThrows(OutboxEventNotFoundException.class,
                () -> stateService.updateOutboxStatus(testOutboxEventId, OutboxEventStatus.PROCESSING));

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void updateOutboxStatus_ShouldThrowIllegalArgumentException_WhenEventIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> stateService.updateOutboxStatus(null, OutboxEventStatus.PROCESSING));

        verify(outboxEventRepository, never()).findById(any());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }
}
