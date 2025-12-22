package com.mymicroservice.orderservice.kafka;

import com.mymicroservice.orderservice.util.KafkaMdcUtil;
import org.mymicroservices.common.events.OrderEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEventDto> kafkaTemplate;
    private final String topic = "create-order";

    private static final Logger TRACE_LOGGER = LoggerFactory.getLogger("TRACE_MDC_LOGGER");

    public void sendCreateOrder(OrderEventDto event, Runnable onSuccess) {

        TRACE_LOGGER.info("Sending CREATE_ORDER for orderId={}", event.getOrderId());
        log.info("Producing CREATE_ORDER for orderId={}", event.getOrderId());

        // creating a message with MDC
        Message<OrderEventDto> message = KafkaMdcUtil.addMdcToMessage(
                event,
                event.getOrderId(),
                topic
        );

        CompletableFuture<SendResult<String, OrderEventDto>> future =
                kafkaTemplate.send(message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("CREATE_ORDER sent for orderId={}, offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().offset());

                TRACE_LOGGER.info("CREATE_ORDER sent for orderId={}, offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().offset());
                onSuccess.run(); // Calling a callback after successful sending => changing the status to PROCESSING
            } else {
                log.error("Failed to send CREATE_ORDER for orderId={}", event.getOrderId(), ex);
                TRACE_LOGGER.error("Failed to send CREATE_ORDER for orderId={}", event.getOrderId(), ex);
            }
        });
    }
}
