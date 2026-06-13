package com.mymicroservice.orderservice.integration.kafka;

import com.mymicroservice.orderservice.configuration.AbstractContainerTest;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.repository.OutboxEventRepository;
import com.mymicroservice.orderservice.scheduler.OutboxScheduler;
import com.mymicroservice.orderservice.scheduler.PartitionScheduler;
import com.mymicroservice.orderservice.service.OutboxService;
import com.mymicroservice.orderservice.util.OrderEventDtoGenerator;
import net.javacrumbs.shedlock.core.LockProvider;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mymicroservice.orderservice.util.data.TestConstants.CREATE_ORDER_TOPIC;
import static com.mymicroservice.orderservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.ORDER_CREATED_EVENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false"
})
@EmbeddedKafka(partitions = 1, topics = {CREATE_ORDER_TOPIC})
@DirtiesContext
@ActiveProfiles("testcontainer")
class OutboxFlowIT extends AbstractContainerTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockBean
    private OutboxScheduler outboxScheduler;

    @MockBean
    private PartitionScheduler partitionScheduler;

    @MockBean
    private LockProvider lockProvider;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void processPendingOutboxEvents_ShouldPublishOrderEventToKafka_WhenOutboxEventIsInitial() {
        OrderEventDto orderEventDto = OrderEventDtoGenerator.generateOrderEventDto(ENTITY_ID);
        outboxService.saveOutboxEvent(orderEventDto, ORDER_CREATED_EVENT_TYPE);

        assertThat(outboxEventRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll().get(0).getStatus()).isEqualTo(OutboxEventStatus.INITIAL);

        outboxService.processPendingOutboxEvents();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(outboxEventRepository.findAll().get(0).getStatus())
                    .isIn(OutboxEventStatus.PROCESSING, OutboxEventStatus.SENT);
        });

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "outbox-flow-test-group", "true", embeddedKafkaBroker);
        consumerProps.put("spring.json.trusted.packages", "*");
        consumerProps.put("spring.json.value.default.type", OrderEventDto.class.getName());
        consumerProps.put("value.deserializer",
                "org.springframework.kafka.support.serializer.JsonDeserializer");
        consumerProps.put("key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");

        ConsumerFactory<String, OrderEventDto> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, OrderEventDto> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList(CREATE_ORDER_TOPIC));

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThan(0);

        ConsumerRecord<String, OrderEventDto> record = records.iterator().next();
        assertThat(record.value().getOrderId()).isEqualTo(ENTITY_ID);
        assertThat(record.value().getPaymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));

        consumer.close();
    }
}
