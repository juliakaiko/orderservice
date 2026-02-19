package com.mymicroservice.orderservice;

import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.scheduler.PartitionScheduler;
import com.mymicroservice.orderservice.service.ItemService;
import com.mymicroservice.orderservice.service.OrderItemService;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderserviceApplicationTests {

    @MockBean private ItemRepository itemRepository;
    @MockBean private OrderRepository orderRepository;
    @MockBean private OrderItemRepository orderItemRepository;
    @MockBean private ItemService itemService;
    @MockBean private OrderItemService orderItemService;
    @MockBean private PartitionScheduler partitionScheduler;
    @MockBean private LockProvider lockProvider;

	@Test
	void contextLoads() {
	}

}
