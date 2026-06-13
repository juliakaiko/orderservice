package com.mymicroservice.orderservice.service;

import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;

import java.util.UUID;

public interface StateService {

    void updateOutboxStatus(UUID eventId, OutboxEventStatus outboxEventStatus);

    void updateOrderStatus(UUID orderId, OrderStatus status);
}
