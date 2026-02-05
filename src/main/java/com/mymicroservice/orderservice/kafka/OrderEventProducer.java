package com.mymicroservice.orderservice.kafka;

import com.mymicroservice.orderservice.util.KafkaMdcUtil;
import org.mymicroservices.common.events.OrderEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


    public void sendCreateOrder(OrderEventDto event, Runnable onSuccess) {

        log.info("Producing CREATE_ORDER for orderId={}", event.getOrderId());

        /**
         * Creating a message with MDC
         */
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
                onSuccess.run(); // Calling a callback after successful sending => changing the status to PROCESSING
            } else {
                log.error("Failed to send CREATE_ORDER for orderId={}", event.getOrderId(), ex);
            }
        });
    }
}
