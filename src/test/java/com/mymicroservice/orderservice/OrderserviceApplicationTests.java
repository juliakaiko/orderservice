package com.mymicroservice.orderservice;

import com.mymicroservice.orderservice.kafka.OrderEventProducer;
import com.mymicroservice.orderservice.kafka.PaymentEventConsumer;
import com.mymicroservice.orderservice.kafka.outbox.OutboxEventProducer;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.repository.OutboxEventRepository;
import com.mymicroservice.orderservice.scheduler.OutboxScheduler;
import com.mymicroservice.orderservice.scheduler.PartitionScheduler;
import com.mymicroservice.orderservice.service.ItemService;
import com.mymicroservice.orderservice.service.OrderItemService;
import com.mymicroservice.orderservice.service.OutboxService;
import com.mymicroservice.orderservice.service.PartitionService;
import com.mymicroservice.orderservice.service.StateService;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false"
})
@ActiveProfiles("test")
class OrderserviceApplicationTests {

    @MockBean private ItemRepository itemRepository;
    @MockBean private OrderRepository orderRepository;
    @MockBean private OrderItemRepository orderItemRepository;
    @MockBean private OutboxEventRepository outboxEventRepository;
    @MockBean private ItemService itemService;
    @MockBean private OrderItemService orderItemService;
    @MockBean private OutboxService outboxService;
    @MockBean private StateService stateService;
    @MockBean private OrderEventProducer orderEventProducer;
    @MockBean private OutboxEventProducer outboxEventProducer;
    @MockBean private PaymentEventConsumer paymentEventConsumer;
    @MockBean private PartitionScheduler partitionScheduler;
    @MockBean private OutboxScheduler outboxScheduler;
    @MockBean private PartitionService partitionService;
    @MockBean private LockProvider lockProvider;

    @Test
    void contextLoads_ShouldStartApplicationContext_WhenTestProfileIsActive() {
    }
}
