package com.mymicroservice.orderservice.kafka;

import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.service.StateService;
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
import java.util.UUID;

import static com.mymicroservice.orderservice.util.CommonConstants.EVENT_TYPE;
import static com.mymicroservice.orderservice.util.CommonConstants.IDEMPOTENCE_ID;
import static com.mymicroservice.orderservice.util.CommonConstants.SOURCE_SERVICE;
import static com.mymicroservice.orderservice.util.CommonConstants.TRACE_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final StateService stateService;

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
            @Header(value = IDEMPOTENCE_ID) String idempotenceId,
            @Header(value = EVENT_TYPE) String eventType,
            @Header(value = TRACE_ID) String traceId,
            @Header(value = SOURCE_SERVICE) String sourceService,
            Acknowledgment ack) {

        setUpMDC(traceId, sourceService);

        try {
            log.info("Received {} event [key: {}, partition: {}, offset: {}]: {}",
                    eventType, key, partition, offset, event);

            if (event == null) {
                log.error("Received null {} event", eventType);
                return;
            }

            if (event.getOrderId() == null) {
                log.error("Order ID is null in {} event: {}", eventType, event);
                return;
            }

            UUID orderId;
            try {
                orderId = UUID.fromString(event.getOrderId());
            } catch (IllegalArgumentException e) {
                log.error("Invalid order ID format: {}", event.getOrderId());
                ack.acknowledge();
                return;
            }

            OrderStatus enumStatus;
            String status = event.getStatus();
            if (status == null) {
                log.error("Status is null in {} event: {}", eventType, event);
                ack.acknowledge();
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

            stateService.updateOrderStatus(orderId, enumStatus);
            ack.acknowledge(); // commit offset

            log.info("Successfully updated order {} status to {}", orderId, enumStatus);

        } catch (Exception e) {
            log.error("Error processing {} event [key: {}, partition: {}, offset: {}]: {}",
                    eventType, key, partition, offset, e.getMessage(), e);
            ack.nack(Duration.ofMillis(100)); // sleep and try again
        }
        finally {
            MDC.clear();
        }
    }

    private void setUpMDC(String traceId, String sourceService) {
        if (traceId != null)
            MDC.put("traceId", traceId);
        if (sourceService != null)
            MDC.put("sourceService", sourceService);
        MDC.put("serviceName", serviceName);
    }
}
