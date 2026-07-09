package com.mymicroservice.orderservice.integration.kafka;

import com.mymicroservice.orderservice.configuration.AbstractContainerTest;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.scheduler.OutboxScheduler;
import com.mymicroservice.orderservice.scheduler.PartitionScheduler;
import com.mymicroservice.orderservice.util.CommonConstants;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.PaymentEventDtoGenerator;
import net.javacrumbs.shedlock.core.LockProvider;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.mymicroservice.orderservice.util.data.TestConstants.CREATE_ORDER_EVENT_TYPE;
import static com.mymicroservice.orderservice.util.data.TestConstants.CREATE_PAYMENT_TOPIC;
import static com.mymicroservice.orderservice.util.data.TestConstants.SERVICE_NAME;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ORDER_UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false"
})
@EmbeddedKafka(partitions = 1, topics = {CREATE_PAYMENT_TOPIC})
@DirtiesContext
@ActiveProfiles("testcontainer")
class PaymentEventConsumerIT extends AbstractContainerTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    @MockBean
    private OutboxScheduler outboxScheduler;

    @MockBean
    private PartitionScheduler partitionScheduler;

    @MockBean
    private LockProvider lockProvider;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        Order order = OrderGenerator.generateOrder();
        order.setId(TEST_ORDER_UUID);
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
    }

    @Test
    void onCreatePayment_ShouldUpdateOrderStatusToPaid_WhenPaidEventReceived() {
        PaymentEventDto event = PaymentEventDtoGenerator.generatePaidPaymentEventDto(TEST_ORDER_UUID.toString());
        sendPaymentEvent(event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(TEST_ORDER_UUID).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
        });
    }

    @Test
    void onCreatePayment_ShouldUpdateOrderStatusToFailed_WhenFailedEventReceived() {
        PaymentEventDto event = PaymentEventDtoGenerator.generateFailedPaymentEventDto(TEST_ORDER_UUID.toString());
        sendPaymentEvent(event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(TEST_ORDER_UUID).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.FAILED);
        });
    }

    private void sendPaymentEvent(PaymentEventDto event) {
        ProducerRecord<String, PaymentEventDto> record =
                new ProducerRecord<>(CREATE_PAYMENT_TOPIC, event.getOrderId(), event);
        record.headers().add(CommonConstants.IDEMPOTENCE_ID,
                com.mymicroservice.orderservice.util.data.TestConstants.IDEMPOTENCE_ID.getBytes(StandardCharsets.UTF_8));
        record.headers().add(CommonConstants.EVENT_TYPE,
                CREATE_ORDER_EVENT_TYPE.getBytes(StandardCharsets.UTF_8));
        record.headers().add(CommonConstants.TRACE_ID,
                com.mymicroservice.orderservice.util.data.TestConstants.TRACE_ID.getBytes(StandardCharsets.UTF_8));
        record.headers().add(CommonConstants.SOURCE_SERVICE,
                SERVICE_NAME.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }
}
