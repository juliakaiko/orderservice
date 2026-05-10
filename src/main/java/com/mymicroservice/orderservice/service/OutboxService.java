package com.mymicroservice.orderservice.service;

import org.mymicroservices.common.events.OrderEventDto;

import java.util.Set;
import java.util.UUID;

public interface OutboxService {

    void saveOutboxEvent(String aggregateId, String eventType, OrderEventDto dto);

    Set<UUID> processPendingOutboxEvents();

}
