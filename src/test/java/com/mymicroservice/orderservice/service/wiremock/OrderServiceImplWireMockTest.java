package com.mymicroservice.orderservice.service.wiremock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.orderservice.client.UserClient;
import com.mymicroservice.orderservice.dto.*;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.kafka.OrderEventProducer;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.*;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.service.OrderService;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.UserGenerator;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 8089)
public class OrderServiceImplWireMockTest {

    @MockBean private OrderRepository orderRepository;
    @MockBean private ItemRepository itemRepository;
    @MockBean private OrderEventProducer orderEventProducer;

    @Autowired private OrderService orderService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WireMockServer wireMockServer;
    @Autowired private UserClient userClient;

    private final UUID TEST_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");;
    private final String TEST_USER_EMAIL = "test@test.by";
    private Order testOrder;
    private OrderDto testOrderDto;
    private UserDto testUserDto;

    @BeforeEach
    void setup() throws Exception {
        testOrder = OrderGenerator.generateOrder();
        testOrder.setId(TEST_ORDER_ID);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(5L);

        Item item = new Item();
        item.setId(2L);
        item.setPrice(BigDecimal.valueOf(100));
        orderItem.setItem(item);
        orderItem.setOrder(testOrder);

        testOrder.setOrderItems(new HashSet<>(Set.of(orderItem)));
        testOrderDto = OrderMapper.INSTANCE.toDto(testOrder);
        testUserDto = UserGenerator.generateUserResponse();

        // WireMock for /api/internal/users/1
        wireMockServer.stubFor(get(urlEqualTo("/api/internal/users/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testUserDto))));

        // WireMock for /api/internal/users/find-by-email
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
    void testGetOrdersByUserEmail_returnsOrders() {
        when(orderRepository.findOrdersByUserId(testUserDto.getUserId()))
                .thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.getOrdersByUserEmail(TEST_USER_EMAIL);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_ORDER_ID, result.get(0).getOrder().getId());
        assertEquals(TEST_USER_EMAIL, result.get(0).getUser().getEmail());

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/internal/users/find-by-email"))
                .withQueryParam("email", equalTo(TEST_USER_EMAIL)));
    }

    @Test
    void testCreateNewOrder() {
        when(itemRepository.findById(2L)).thenReturn(Optional.of(new Item()));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderWithUserResponse result = orderService.createOrder(testOrderDto);

        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.getOrder().getId());
    }

    @Test
    void testGetOrderById_whenExists() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        OrderWithUserResponse result = orderService.getOrderById(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.getOrder().getId());
        assertEquals(TEST_USER_EMAIL, result.getUser().getEmail());

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/internal/users/1")));
    }

    @Test
    void testGetOrderById_whenNotExist() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(TEST_ORDER_ID));

        wireMockServer.verify(0, getRequestedFor(urlEqualTo("/api/internal/users/1")));
    }

    @Test
    void testUpdateOrder_whenExists() {
        Order updatedOrder = OrderGenerator.generateOrder();
        updatedOrder.setId(TEST_ORDER_ID);
        updatedOrder.setStatus(OrderStatus.PROCESSING);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(5L);

        Item item = new Item();
        item.setId(2L);
        item.setPrice(BigDecimal.valueOf(100));
        orderItem.setItem(item);
        orderItem.setOrder(updatedOrder);

        updatedOrder.setOrderItems(new HashSet<>(Set.of(orderItem)));
        testOrder.setOrderItems(new HashSet<>(Set.of(orderItem)));

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        doAnswer(invocation -> {
            Runnable callback = invocation.getArgument(1);
            callback.run();
            return null;
        }).when(orderEventProducer).sendCreateOrder(any(OrderEventDto.class), any(Runnable.class));

        OrderDto updateDto = OrderMapper.INSTANCE.toDto(updatedOrder);
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItemId(2L);
        orderItemDto.setQuantity(10L);
        updateDto.setOrderItems(Set.of(orderItemDto));

        OrderWithUserResponse result = orderService.updateOrder(TEST_ORDER_ID, updateDto);

        assertNotNull(result);
        assertEquals(updateDto.getUserId(), result.getOrder().getUserId());

        verify(orderEventProducer, times(1)).sendCreateOrder(any(OrderEventDto.class), any(Runnable.class));
        verify(orderRepository, times(2)).save(any(Order.class));

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/internal/users/1")));
    }

    @Test
    void testDeleteOrder_whenExists() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.deleteOrder(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.getId());
        verify(orderRepository, times(1)).deleteById(TEST_ORDER_ID);
    }
}