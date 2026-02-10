package com.mymicroservice.orderservice.kafka;

import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.PaymentEventDto;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderService orderService;
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String SOURCE_SERVICE_HEADER = "X-Source-Service";

    @Value("${spring.application.name}")
    private String serviceName;

    @KafkaListener(
            topics = "${kafka.consumer.topics.create-payment}",
            groupId = "${kafka.consumer.group-id}"
    )
    public void onCreatePayment(
            @Payload PaymentEventDto event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(REQUEST_ID_HEADER) String requestId,
            @Header(SOURCE_SERVICE_HEADER) String sourceService,
            Acknowledgment ack) {

        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        if (sourceService != null) {
            MDC.put("sourceService", sourceService);
        }
        MDC.put("serviceName", serviceName);

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
            ack.acknowledge(); // commit offset

            log.info("Successfully updated order {} status to {}", orderId, enumStatus);

        } catch (Exception e) {
            log.error("Error processing CREATE_PAYMENT event [key: {}, partition: {}, offset: {}]: {}",
                    key, partition, offset, e.getMessage(), e);
            ack.nack(Duration.ofMillis(100)); // sleep and try again
        }
        finally {
            MDC.clear();
        }
    }
}
