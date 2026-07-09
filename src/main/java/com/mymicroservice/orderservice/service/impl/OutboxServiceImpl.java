package com.mymicroservice.orderservice.service.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import com.mymicroservice.orderservice.kafka.outbox.OutboxEventProducer;
import com.mymicroservice.orderservice.mapper.JsonMapper;
import com.mymicroservice.orderservice.model.OutboxEvent;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.repository.OutboxEventRepository;
import com.mymicroservice.orderservice.service.OutboxService;
import com.mymicroservice.orderservice.service.StateService;
import com.mymicroservice.orderservice.util.MdcUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.OrderEventDto;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {

    private final JsonMapper jsonMapper;
    private final OutboxEventProducer kafkaEventPublisher;
    private final OutboxEventRepository outboxEventRepository;
    private final StateService stateService;

    @Value("${outbox.processing-timeout-minutes:5}")
    private int processingTimeoutMinutes;

    @Value("${outbox.batch-size:100}")
    private int batchSize;

    @Override
    @Transactional
    public void saveOutboxEvent(OrderEventDto orderEventDto, String eventType) {
        jsonMapper.toJson(orderEventDto)
                .ifPresentOrElse(payload -> {
                    OutboxEvent event = OutboxEvent.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .aggregateId(orderEventDto.getOrderId())
                            .eventType(eventType)
                            .payload(payload)
                            .status(OutboxEventStatus.INITIAL)
                            .traceId(MDC.get("traceId"))
                            .sourceService(MDC.get("serviceName"))
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    outboxEventRepository.save(event);
                }, () -> {
                    log.error("Failed to serialize OrderEventDto to JSON. orderId={}", orderEventDto.getOrderId());
                    throw new IllegalStateException("Failed to serialize OrderEventDto");
                });
    }

    @Override
    @Transactional
    public void processPendingOutboxEvents() {
        recoverStaleProcessingEvents();

        List<OutboxEvent> events = outboxEventRepository.findEventsForProcessing(
                List.of(OutboxEventStatus.INITIAL.name(), OutboxEventStatus.FAILED.name()),
                batchSize
        );

        for (OutboxEvent event : events) {
            try {
                MdcUtils.runWithOutboxEvent(event, () -> {
                    stateService.updateOutboxStatus(event.getId(), OutboxEventStatus.PROCESSING);
                    kafkaEventPublisher.publishOutboxEvent(event);
                    log.info("Outbox event processed successfully. id={}", event.getId());
                });
            } catch (Exception ex) {
                log.error("Failed to publish event id={}", event.getId(), ex);
                markOutboxAsFailed(event.getId());
            }
        }
    }

    private void recoverStaleProcessingEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
        int recovered = outboxEventRepository.resetStaleProcessingEvents(threshold);
        if (recovered > 0) {
            log.warn("Recovered {} stale PROCESSING outbox events (reset to FAILED)", recovered);
        }
    }

    private void markOutboxAsFailed(java.util.UUID eventId) {
        try {
            stateService.updateOutboxStatus(eventId, OutboxEventStatus.FAILED);
        } catch (Exception statusEx) {
            log.error("Failed to mark outbox event as FAILED. id={}", eventId, statusEx);
        }
    }
}
