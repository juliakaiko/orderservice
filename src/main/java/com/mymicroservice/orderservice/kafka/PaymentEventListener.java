package com.mymicroservice.orderservice.kafka;

import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "create-payment", groupId = "order-service-group")
    public void onCreatePayment(
            @Payload PaymentEventDto event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            log.info("Received CREATE_PAYMENT event [key: {}, partition: {}, offset: {}]: {}",
                    key, partition, offset, event);

            if (event == null) {
                log.error("Received null CREATE_PAYMENT event");
                return;
            }

            if (event.getOrderId() == null) {
                log.error("Order ID is null in CREATE_PAYMENT event: {}", event);
                return;
            }

            Long orderId;
            try {
                orderId = Long.valueOf(event.getOrderId());
            } catch (NumberFormatException e) {
                log.error("Invalid order ID format: {}", event.getOrderId());
                return;
            }

            OrderStatus enumStatus;
            String status = event.getStatus();
            if (status == null) {
                log.error("Status is null in CREATE_PAYMENT event: {}", event);
                return;
            }

            switch (status) {
                case "PAID":
                    enumStatus = OrderStatus.PAID;
                    break;
                case "FAILED":
                    enumStatus = OrderStatus.FAILED;
                    break;
                default:
                    log.warn("Unknown payment status: {}, defaulting to FAILED", status);
                    enumStatus = OrderStatus.FAILED;
                    break;
            }

            orderService.updateOrderStatus(orderId, enumStatus);
            log.info("Successfully updated order {} status to {}", orderId, enumStatus);

        } catch (Exception e) {
            log.error("Error processing CREATE_PAYMENT event [key: {}, partition: {}, offset: {}]: {}",
                    key, partition, offset, e.getMessage(), e);
        }
    }
}
