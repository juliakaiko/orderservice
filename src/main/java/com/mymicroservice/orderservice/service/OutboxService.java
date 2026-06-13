package com.mymicroservice.orderservice.service;

import org.mymicroservices.common.events.OrderEventDto;

public interface OutboxService {

    void saveOutboxEvent(OrderEventDto orderEventDto, String eventType);

    void processPendingOutboxEvents();
}
