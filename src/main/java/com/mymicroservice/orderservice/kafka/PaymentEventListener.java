package com.mymicroservice.orderservice.kafka;

import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "create-payment", groupId = "order-service-group")
    public void onCreatePayment(PaymentEventDto event) {
        log.info("Received CREATE_PAYMENT event: {}", event);
        if (event == null || event.getStatus() == null) {
            log.warn("Received invalid CREATE_PAYMENT event: {}", event);
            return;
        }
        Long orderId = Long.valueOf(event.getOrderId());

        String status = event.getStatus();
        OrderStatus enumStatus = OrderStatus.PROCESSING;
        switch (status){
            case "PAID":
                enumStatus = OrderStatus.PAID;
                break;
            case "FAILED":
                enumStatus = OrderStatus.FAILED;
                break;
            default:
                enumStatus = OrderStatus.FAILED;
                break;
        }
        orderService.updateOrderStatus(orderId, enumStatus);
    }
}
