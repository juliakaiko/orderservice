package com.mymicroservice.orderservice.service.wiremock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.mymicroservice.orderservice.client.UserClient;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserDto;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.kafka.OrderEventProducer;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.service.OrderService;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.UserGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWireMock(port = 0) // WireMock will work on a random port
@ActiveProfiles("test")
public class OrderServiceImplWireMockTest {

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private ItemRepository itemRepository;

    @MockBean
    private OrderEventProducer orderEventProducer;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserClient userClient;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("user-service.url", () -> "http://localhost:${wiremock.server.port}");
    }

    private static final Long TEST_ORDER_ID = 1L;
    private static final String TEST_USER_EMAIL = "test@test.by";
    private Order testOrder;
    private OrderDto testOrderDto;
    private UserDto testUserDto;
    private OrderWithUserResponse testOrderWithUserResponse;

    @BeforeEach
    void setUp() throws Exception {
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

        //testOrder.setOrderItems(Set.of(orderItem));
        testOrder.setOrderItems(new HashSet<>(Set.of(orderItem)));

        testOrderDto = OrderMapper.INSTANCE.toDto(testOrder);

        testUserDto = UserGenerator.generateUserResponse();
        testOrderWithUserResponse = new OrderWithUserResponse(testOrderDto, testUserDto);

        // Stub for getting user by ID
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/internal/users/1"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testUserDto))));
    }

    @AfterEach
    void tearDown() {
        WireMock.reset(); // Resetting WireMock state after each test
    }

    @Test
    void testCreateNewOrder_ReturnsOrderWithUserResponse() {

        Item mockItem = new Item();
        mockItem.setId(2L);
        mockItem.setPrice(BigDecimal.valueOf(100));

        when(itemRepository.findById(2L)).thenReturn(Optional.of(mockItem));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderWithUserResponse result = orderService.createOrder(testOrderDto);

        assertNotNull(result);
        assertEquals(testOrderWithUserResponse.getOrder(), result.getOrder());
        assertEquals(testOrderWithUserResponse.getUser(), result.getUser());

        verify(orderEventProducer, times(1))
                .sendCreateOrder(any(OrderEventDto.class), any(Runnable.class));

        verifyFeignCall("/api/internal/users/1");
    }

    @Test
    void testGetOrderById_whenIdExists_thenReturnsOrderWithUserResponse() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        OrderWithUserResponse result = orderService.getOrderById(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(testOrderWithUserResponse.getOrder(), result.getOrder());
        assertEquals(testOrderWithUserResponse.getUser(), result.getUser());

        verifyFeignCall("/api/internal/users/1");
    }

    @Test
    void testGetOrderById_whenIdNotExist_thenNotFound() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(TEST_ORDER_ID);
        });

        WireMock.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/internal/users/1")));
    }

    @Test
    void testUpdateOrder_whenIdExists_thenReturnsOrderWithUserResponse() {
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
        orderItem.setOrder(testOrder);

        updatedOrder.setOrderItems(new HashSet<>(Set.of(orderItem)));

        testOrder.setOrderItems(new HashSet<>(Set.of(orderItem)));

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        // Mock for orderEventProducer
        org.mockito.Mockito.doAnswer(invocation -> {
            Runnable callback = invocation.getArgument(1);
            callback.run(); // Performing a callback to update the status
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

        verify(orderEventProducer, times(1))
                .sendCreateOrder(any(OrderEventDto.class), any(Runnable.class));

        // check that save was called twice (main update + status update when callback)
        verify(orderRepository, times(2)).save(any(Order.class));

        verifyFeignCall("/api/internal/users/1");
    }

    //no Feign here
    @Test
    void testDeleteOrder_whenIdExists_thenDeletesAndReturnsOrderDto(){
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.deleteOrder(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(testOrderDto.getId(), result.getId());
        verify(orderRepository, times(1)).deleteById(TEST_ORDER_ID);
    }

    @Test
    void testGetOrdersByUserEmail_whenEmailExists_thenReturnsOrdersWithUser() throws JsonProcessingException {
        List<Order> orders = List.of(testOrder);
        when(orderRepository.findOrdersByUserId(testUserDto.getUserId()))
                .thenReturn(orders);

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/internal/users/find-by-email"))
                .withQueryParam("email", WireMock.matching("test(.+)test\\.by"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testUserDto))));

        List<OrderWithUserResponse> result = orderService.getOrdersByUserEmail(TEST_USER_EMAIL);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrderDto.getId(), result.get(0).getOrder().getId());
        assertEquals(TEST_USER_EMAIL, result.get(0).getUser().getEmail());

        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/internal/users/find-by-email"))
                .withQueryParam("email", WireMock.matching("test(.+)test\\.by")));
    }

    @Test
    void testGetOrdersIdIn_whenIdsExists_thenReturnsOrdersWithUsers() {
        Set<Long> ids = Set.of(TEST_ORDER_ID);
        when(orderRepository.findAllByIdIn(ids)).thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.getOrdersIdIn(ids);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrderDto.getId(), result.get(0).getOrder().getId());

        verifyFeignCall("/api/internal/users/1");
    }

    @Test
    void testFindByStatusIn_whenStatusesExists_thenReturnsOrdersWithUsers()  {
        Set<OrderStatus> statuses = Set.of(OrderStatus.CREATED);
        when(orderRepository.findByStatusIn(statuses)).thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.findByStatusIn(statuses);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrderDto.getId(), result.get(0).getOrder().getId());

        verifyFeignCall("/api/internal/users/1");
    }

    @Test
    void testGetAllOrders_thenReturnsAllOrdersWithUsers()  {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrderDto.getId(), result.get(0).getOrder().getId());

        verifyFeignCall("/api/internal/users/1");
    }

    @Test
    void testGetAllOrdersNativeWithPagination_thenReturnsPagedOrderDtos() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findAllOrdersNative(pageable)).thenReturn(page);

        Page<OrderDto> result = orderService.getAllOrdersNativeWithPagination(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testOrderDto.getId(), result.getContent().get(0).getId());
    }

    private void verifyFeignCall(String url) {
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(url)));
    }
}
