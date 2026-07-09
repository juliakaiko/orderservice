package com.mymicroservice.orderservice.scheduler;

import com.mymicroservice.orderservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 1000)
    public void processOutbox() {
        outboxService.processPendingOutboxEvents();
    }
}
