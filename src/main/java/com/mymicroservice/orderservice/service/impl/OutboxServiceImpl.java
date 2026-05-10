package com.mymicroservice.orderservice.service.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import com.mymicroservice.orderservice.kafka.outbox.OutboxEventPublisher;
import com.mymicroservice.orderservice.mapper.JsonMapper;
import com.mymicroservice.orderservice.model.OutboxEvent;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.repository.OutboxEventRepository;
import com.mymicroservice.orderservice.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.mymicroservices.common.events.OrderEventDto;
import org.slf4j.MDC;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {

    private final JsonMapper jsonMapper;
    private final OutboxEventPublisher kafkaEventPublisher;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public void saveOutboxEvent(String aggregateId, String eventType, OrderEventDto orderEventDto) {
        OutboxEvent event = OutboxEvent.builder()
                .eventId(UuidCreator.getTimeOrderedEpoch())
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(jsonMapper.toJson(orderEventDto))
                .status(OutboxEventStatus.INITIAL)
                .requestId(MDC.get("requestId"))
                .sourceService(MDC.get("serviceName"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        outboxEventRepository.save(event);
    }

    @Override
    @Transactional
    public Set<UUID> processPendingOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findEventsForProcessing(
                            List.of(OutboxEventStatus.INITIAL.name()),
                        100
        );
        Set<UUID> processedOrdersIds = new HashSet<>();
        for (OutboxEvent event : events) {
            try {
                event.setStatus(OutboxEventStatus.PROCESSED);
                event.setUpdatedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                kafkaEventPublisher.publish(event);

                event.setStatus(OutboxEventStatus.SENT);
                event.setProcessedAt(LocalDateTime.now());
                processedOrdersIds.add(UUID.fromString(event.getAggregateId()));
            } catch (Exception ex) {
                event.setStatus(OutboxEventStatus.FAILED);
                log.error("Failed to publish event id={}", event.getEventId(), ex);
            }
            event.setUpdatedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
        }
        return processedOrdersIds;
    }
}
