package com.mymicroservice.orderservice.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserDto;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.kafka.OrderEventProducer;
import com.mymicroservice.orderservice.kafka.PaymentEventConsumer;
import com.mymicroservice.orderservice.kafka.outbox.OutboxEventProducer;
import com.mymicroservice.orderservice.repository.OrderItemRepository;
import com.mymicroservice.orderservice.repository.OutboxEventRepository;
import com.mymicroservice.orderservice.scheduler.OutboxScheduler;
import com.mymicroservice.orderservice.service.ItemService;
import com.mymicroservice.orderservice.service.OrderItemService;
import com.mymicroservice.orderservice.service.StateService;
import com.mymicroservice.orderservice.scheduler.PartitionScheduler;
import com.mymicroservice.orderservice.service.OrderService;
import com.mymicroservice.orderservice.service.PartitionService;
import com.mymicroservice.orderservice.service.OutboxService;
import com.mymicroservice.orderservice.security.OrderAuthorizationService;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.UserGenerator;
import com.mymicroservice.orderservice.client.UserClient;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ITEM_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ORDER_UUID;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 8089)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
class OrderServiceWireMockIT {

    @MockBean private OrderRepository orderRepository;
    @MockBean private ItemRepository itemRepository;
    @MockBean private OrderItemRepository orderItemRepository;
    @MockBean private OutboxService outboxService;
    @MockBean private OrderAuthorizationService orderAuthorizationService;
    @MockBean private StateService stateService;
    @MockBean private OutboxEventRepository outboxEventRepository;
    @MockBean private OrderEventProducer orderEventProducer;
    @MockBean private OutboxEventProducer outboxEventProducer;
    @MockBean private PaymentEventConsumer paymentEventConsumer;
    @MockBean private ItemService itemService;
    @MockBean private OrderItemService orderItemService;
    @MockBean private PartitionScheduler partitionScheduler;
    @MockBean private OutboxScheduler outboxScheduler;
    @MockBean private LockProvider lockProvider;
    @MockBean private PartitionService partitionService;

    @Autowired private OrderService orderService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WireMockServer wireMockServer;
    @Autowired private UserClient userClient;

    private Order testOrder;
    private OrderDto testOrderDto;
    private UserDto testUserDto;

    @BeforeEach
    void setup() throws Exception {
        testOrder = OrderGenerator.generateOrder();
        testOrder.setId(TEST_ORDER_UUID);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(5L);

        Item item = new Item();
        item.setId(TEST_ITEM_ID);
        item.setPrice(BigDecimal.valueOf(100));
        orderItem.setItem(item);
        orderItem.setOrder(testOrder);

        testOrder.setOrderItems(new HashSet<>(Set.of(orderItem)));
        testOrderDto = OrderMapper.INSTANCE.toDto(testOrder);
        testUserDto = UserGenerator.generateUserResponse();

        wireMockServer.stubFor(get(urlEqualTo("/api/internal/users/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testUserDto))));

        wireMockServer.stubFor(get(urlPathEqualTo("/api/internal/users/find-by-email"))
                .withQueryParam("email", equalTo(TEST_USER_EMAIL))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testUserDto))));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
    }

    @Test
    void getOrdersByUserEmail_ShouldReturnOrders_WhenUserServiceResponds() {
        when(orderRepository.findOrdersByUserId(testUserDto.getUserId()))
                .thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.getOrdersByUserEmail(TEST_USER_EMAIL);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_ORDER_UUID, result.get(0).getOrder().getId());
        assertEquals(TEST_USER_EMAIL, result.get(0).getUser().getEmail());

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/internal/users/find-by-email"))
                .withQueryParam("email", equalTo(TEST_USER_EMAIL)));
    }

    @Test
    void createOrder_ShouldReturnOrderWithUser_WhenOrderIsValid() {
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(new Item()));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderWithUserResponse result = orderService.createOrder(testOrderDto);

        assertNotNull(result);
        assertEquals(TEST_ORDER_UUID, result.getOrder().getId());
        verify(outboxService, times(1)).saveOutboxEvent(any(OrderEventDto.class), anyString());
    }

    @Test
    void getOrderById_ShouldReturnOrderWithUser_WhenOrderExists() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));

        OrderWithUserResponse result = orderService.getOrderById(TEST_ORDER_UUID);

        assertNotNull(result);
        assertEquals(TEST_ORDER_UUID, result.getOrder().getId());
        assertEquals(TEST_USER_EMAIL, result.getUser().getEmail());
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/internal/users/1")));
    }

    @Test
    void getOrderById_ShouldThrowOrderNotFoundException_WhenOrderNotExists() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(TEST_ORDER_UUID));
        wireMockServer.verify(0, getRequestedFor(urlEqualTo("/api/internal/users/1")));
    }

    @Test
    void updateOrder_ShouldReturnUpdatedOrder_WhenOrderExists() {
        Order updatedOrder = OrderGenerator.generateOrder();
        updatedOrder.setId(TEST_ORDER_UUID);
        updatedOrder.setStatus(OrderStatus.CANCELLED);

        Item item = new Item();
        item.setId(TEST_ITEM_ID);
        item.setPrice(BigDecimal.valueOf(100));

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(5L);
        orderItem.setItem(item);
        orderItem.setOrder(updatedOrder);
        updatedOrder.setOrderItems(new HashSet<>(Set.of(orderItem)));
        testOrder.setOrderItems(new HashSet<>(Set.of(orderItem)));

        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(itemRepository.findById(any())).thenReturn(Optional.of(item));

        OrderDto updateDto = OrderMapper.INSTANCE.toDto(updatedOrder);
        OrderItemDto orderItemDto = OrderItemDto.builder().itemId(TEST_ITEM_ID).quantity(10L).build();
        updateDto.setOrderItems(Set.of(orderItemDto));

        OrderWithUserResponse result = orderService.updateOrder(TEST_ORDER_UUID, updateDto);

        assertNotNull(result);
        assertEquals(updateDto.getUserId(), result.getOrder().getUserId());
        verify(outboxService, never()).saveOutboxEvent(any(OrderEventDto.class), anyString());
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/internal/users/1")));
    }

    @Test
    void deleteOrder_ShouldReturnOrderDto_WhenOrderExists() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.deleteOrder(TEST_ORDER_UUID);

        assertNotNull(result);
        assertEquals(TEST_ORDER_UUID, result.getId());
        verify(orderRepository, times(1)).deleteById(TEST_ORDER_UUID);
    }
}
