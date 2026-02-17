package com.mymicroservice.orderservice.service.wiremock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.orderservice.client.UserClient;
import com.mymicroservice.orderservice.dto.*;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.kafka.OrderEventProducer;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.*;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.service.OrderService;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.UserGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class OrderServiceImplWireMockTest2 {

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private ItemRepository itemRepository;

    @MockBean
    private OrderEventProducer orderEventProducer;

    @MockBean
    private UserClient userClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID TEST_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String TEST_USER_EMAIL = "test@test.by";
    private Order testOrder;
    private OrderDto testOrderDto;
    private UserDto testUserDto;
    private OrderWithUserResponse testOrderWithUserResponse;

    @BeforeEach
    void setup() {
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
        testOrderWithUserResponse = new OrderWithUserResponse(testOrderDto, testUserDto);

        when(userClient.getUserById(anyLong())).thenReturn(testUserDto);
        when(userClient.getUserByEmail(TEST_USER_EMAIL)).thenReturn(testUserDto);
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
        assertEquals(testOrderDto.getId(), result.getOrder().getId());
        assertEquals(testUserDto.getEmail(), result.getUser().getEmail());

        verify(orderEventProducer, times(1))
                .sendCreateOrder(any(), any(Runnable.class));
        verify(userClient, times(1)).getUserById(anyLong());
    }

    @Test
    void testGetOrderById_whenIdExists_thenReturnsOrderWithUserResponse() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        OrderWithUserResponse result = orderService.getOrderById(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(testOrderDto.getId(), result.getOrder().getId());
        assertEquals(testUserDto.getEmail(), result.getUser().getEmail());

        verify(userClient, times(1)).getUserById(anyLong());
    }

    @Test
    void testGetOrderById_whenIdNotExist_thenThrows() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderById(TEST_ORDER_ID));

        verify(userClient, times(0)).getUserById(anyLong());
    }

    @Test
    void testUpdateOrder_whenIdExists_thenReturnsOrderWithUserResponse() {
        Order updatedOrder = OrderGenerator.generateOrder();
        updatedOrder.setId(TEST_ORDER_ID);
        updatedOrder.setStatus(OrderStatus.PROCESSING);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(10L);

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
        }).when(orderEventProducer).sendCreateOrder(any(), any(Runnable.class));

        OrderDto updateDto = OrderMapper.INSTANCE.toDto(updatedOrder);

        OrderWithUserResponse result = orderService.updateOrder(TEST_ORDER_ID, updateDto);

        assertNotNull(result);
        assertEquals(testUserDto.getEmail(), result.getUser().getEmail());

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderEventProducer, times(1)).sendCreateOrder(any(), any(Runnable.class));
        verify(userClient, times(1)).getUserById(anyLong());
    }

    @Test
    void testDeleteOrder_whenIdExists_thenDeletes() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.deleteOrder(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.getId());
        verify(orderRepository, times(1)).deleteById(TEST_ORDER_ID);
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

        verify(userClient, times(1)).getUserByEmail(TEST_USER_EMAIL);
    }

    @Test
    void testGetOrdersIdIn_returnsOrders() {
        Set<UUID> ids = Set.of(TEST_ORDER_ID);
        when(orderRepository.findAllByIdIn(ids)).thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.getOrdersIdIn(ids);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_ORDER_ID, result.get(0).getOrder().getId());
        verify(userClient, times(1)).getUserById(anyLong());
    }

    @Test
    void testFindByStatusIn_returnsOrders() {
        when(orderRepository.findByStatusIn(Set.of(OrderStatus.CREATED)))
                .thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.findByStatusIn(Set.of(OrderStatus.CREATED));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_ORDER_ID, result.get(0).getOrder().getId());
        verify(userClient, times(1)).getUserById(anyLong());
    }

    @Test
    void testGetAllOrders_returnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_ORDER_ID, result.get(0).getOrder().getId());
        verify(userClient, times(1)).getUserById(anyLong());
    }
}
