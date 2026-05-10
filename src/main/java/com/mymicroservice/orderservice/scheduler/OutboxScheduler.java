package com.mymicroservice.orderservice.scheduler;

import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.service.OrderService;
import com.mymicroservice.orderservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxService outboxService;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 1000)
    public void processOutbox() {
        Set<UUID> processedOrdersIds = outboxService.processPendingOutboxEvents();
        /// /!!!! исправить
        orderService.updateOrdersListStatus(processedOrdersIds, OrderStatus.PROCESSING);
    }
}
