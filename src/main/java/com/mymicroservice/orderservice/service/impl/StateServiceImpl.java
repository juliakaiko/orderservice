package com.mymicroservice.orderservice.service.impl;

import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.exception.OutboxEventNotFoundException;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OutboxEvent;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.repository.OutboxEventRepository;
import com.mymicroservice.orderservice.service.StateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StateServiceImpl implements StateService {

    private final OutboxEventRepository outboxEventRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void updateOutboxStatus(UUID eventId, OutboxEventStatus outboxEventStatus) {

        if (eventId == null) {
            throw new IllegalArgumentException("eventId cannot be null");
        }

        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new OutboxEventNotFoundException("OutboxEvent wasn't found with id " + eventId));
        event.setStatus(outboxEventStatus);
        event.setProcessedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        outboxEventRepository.save(event);
        log.info("OutboxEvent with id {} was updated with status {}", eventId, outboxEventStatus);
    }

    @Override
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId));
        order.setStatus(status);
        orderRepository.save(order);
        log.info("Order with id {} was updated with status {}", orderId, status);
    }
}
