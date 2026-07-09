package com.mymicroservice.orderservice.unit.service;

import com.mymicroservice.orderservice.client.UserClient;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserDto;
import com.mymicroservice.orderservice.exception.OrderAlreadyPaidException;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.security.OrderAuthorizationService;
import com.mymicroservice.orderservice.service.OutboxService;
import com.mymicroservice.orderservice.service.impl.OrderServiceImpl;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.UserGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.mymicroservice.orderservice.util.data.TestConstants.ORDER_CREATED_EVENT_TYPE;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ITEM_ID;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ORDER_UUID;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UserClient userClient;

    @Mock
    private OrderAuthorizationService orderAuthorizationService;

    private Order testOrder;
    private OrderDto testOrderDto;
    private UserDto testUserDto;
    private OrderWithUserResponse testOrderWithUserResponse;

    @BeforeEach
    void setUp() {
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

        testOrder.setOrderItems(Set.of(orderItem));
        testOrderDto = OrderMapper.INSTANCE.toDto(testOrder);
        testUserDto = UserGenerator.generateUserResponse();
        testOrderWithUserResponse = new OrderWithUserResponse(testOrderDto, testUserDto);

        lenient().when(orderAuthorizationService.getCurrentUserIdIfNotAdmin()).thenReturn(Optional.empty());
    }

    @Test
    void createOrder_ShouldReturnOrderWithUserResponse_WhenOrderIsValid() {
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testOrder.getOrderItems().iterator().next().getItem()));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserDto);

        OrderWithUserResponse result = orderService.createOrder(testOrderDto);

        assertNotNull(result);
        assertEquals(testOrderWithUserResponse.getOrder(), result.getOrder());
        assertEquals(testOrderWithUserResponse.getUser(), result.getUser());

        verify(orderAuthorizationService).verifyCanCreateOrderForUser(testOrderDto.getUserId());
        InOrder inOrder = inOrder(userClient, orderRepository, outboxService);
        inOrder.verify(userClient).getUserById(testOrderDto.getUserId());
        inOrder.verify(orderRepository).save(any(Order.class));
        inOrder.verify(outboxService).saveOutboxEvent(any(OrderEventDto.class), eq(ORDER_CREATED_EVENT_TYPE));

        ArgumentCaptor<OrderEventDto> eventCaptor = ArgumentCaptor.forClass(OrderEventDto.class);
        verify(outboxService).saveOutboxEvent(eventCaptor.capture(), eq(ORDER_CREATED_EVENT_TYPE));
        assertEquals(TEST_ORDER_UUID.toString(), eventCaptor.getValue().getOrderId());
        assertEquals(BigDecimal.valueOf(500), eventCaptor.getValue().getPaymentAmount());
    }

    @Test
    void createOrder_ShouldCalculateZeroAmount_WhenOrderItemsAreNull() {
        Order orderWithoutItems = OrderGenerator.generateOrder();
        orderWithoutItems.setId(TEST_ORDER_UUID);
        orderWithoutItems.setOrderItems(null);
        OrderDto orderDtoWithoutItems = OrderMapper.INSTANCE.toDto(orderWithoutItems);
        orderDtoWithoutItems.setOrderItems(null);

        when(orderRepository.save(any(Order.class))).thenReturn(orderWithoutItems);
        when(userClient.getUserById(orderDtoWithoutItems.getUserId())).thenReturn(testUserDto);

        orderService.createOrder(orderDtoWithoutItems);

        ArgumentCaptor<OrderEventDto> eventCaptor = ArgumentCaptor.forClass(OrderEventDto.class);
        verify(outboxService).saveOutboxEvent(eventCaptor.capture(), eq(ORDER_CREATED_EVENT_TYPE));
        assertEquals(BigDecimal.ZERO, eventCaptor.getValue().getPaymentAmount());
    }

    @Test
    void getOrderById_ShouldReturnOrderWithUserResponse_WhenIdExists() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserDto);

        OrderWithUserResponse result = orderService.getOrderById(TEST_ORDER_UUID);

        assertNotNull(result);
        assertEquals(testOrderDto, result.getOrder());
        assertEquals(testUserDto, result.getUser());

        verify(orderAuthorizationService).verifyOrderOwnership(testOrder.getUserId());
        verify(orderRepository, times(1)).findById(TEST_ORDER_UUID);
        verify(userClient, times(1)).getUserById(testOrderDto.getUserId());
    }

    @Test
    void getOrderById_ShouldThrowOrderNotFoundException_WhenIdNotExist() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(TEST_ORDER_UUID));

        verify(orderRepository, times(1)).findById(TEST_ORDER_UUID);
        verifyNoInteractions(userClient);
        verify(orderAuthorizationService, never()).verifyOrderOwnership(any());
    }

    @Test
    void updateOrder_ShouldReturnUpdatedOrderWithUserResponse_WhenIdExists() {
        OrderDto updatedOrderDto = new OrderDto();
        updatedOrderDto.setUserId(testOrderDto.getUserId());
        updatedOrderDto.setStatus(OrderStatus.CANCELLED);
        updatedOrderDto.setCreationDate(LocalDateTime.of(2023, 3, 3, 0, 0));

        Order updatedOrder = OrderMapper.INSTANCE.toEntity(updatedOrderDto);
        updatedOrder.setId(TEST_ORDER_UUID);
        updatedOrder.setOrderItems(testOrder.getOrderItems());

        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(userClient.getUserById(updatedOrderDto.getUserId())).thenReturn(testUserDto);

        OrderWithUserResponse result = orderService.updateOrder(TEST_ORDER_UUID, updatedOrderDto);

        assertNotNull(result);
        assertEquals(updatedOrderDto.getStatus(), result.getOrder().getStatus());

        verify(orderAuthorizationService).verifyOrderOwnership(testOrder.getUserId());
        verify(orderRepository, times(1)).findById(TEST_ORDER_UUID);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(userClient, times(1)).getUserById(updatedOrderDto.getUserId());
        verifyNoInteractions(outboxService);
    }

    @Test
    void updateOrder_ShouldThrowOrderAlreadyPaidException_WhenOrderIsPaid() {
        testOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));

        assertThrows(OrderAlreadyPaidException.class,
                () -> orderService.updateOrder(TEST_ORDER_UUID, testOrderDto));

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(outboxService);
    }

    @Test
    void updateOrder_ShouldThrowOrderNotFoundException_WhenIdNotExist() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.updateOrder(TEST_ORDER_UUID, testOrderDto));

        verify(orderRepository, times(1)).findById(TEST_ORDER_UUID);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(userClient);
        verifyNoInteractions(outboxService);
    }

    @Test
    void deleteOrder_ShouldReturnOrderDto_WhenIdExists() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.deleteOrder(TEST_ORDER_UUID);

        assertNotNull(result);
        assertEquals(testOrderDto, result);

        verify(orderAuthorizationService).verifyOrderOwnership(testOrder.getUserId());
        verify(orderRepository, times(1)).findById(TEST_ORDER_UUID);
        verify(orderRepository, times(1)).deleteById(TEST_ORDER_UUID);
    }

    @Test
    void deleteOrder_ShouldThrowOrderNotFoundException_WhenIdNotExist() {
        when(orderRepository.findById(TEST_ORDER_UUID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrder(TEST_ORDER_UUID));

        verify(orderRepository, times(1)).findById(TEST_ORDER_UUID);
        verify(orderRepository, never()).deleteById(TEST_ORDER_UUID);
    }

    @Test
    void getOrdersByUserEmail_ShouldReturnOrdersWithUser_WhenEmailExists() {
        when(userClient.getUserByEmail(anyString())).thenReturn(testUserDto);
        when(orderRepository.findOrdersByUserId(testUserDto.getUserId())).thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> results = orderService.getOrdersByUserEmail("test@example.com");

        assertFalse(results.isEmpty());
        assertEquals(testUserDto, results.get(0).getUser());

        verify(orderAuthorizationService).verifyCanAccessUserEmail("test@example.com");
        verify(userClient, times(1)).getUserByEmail(anyString());
        verify(orderRepository, times(1)).findOrdersByUserId(testUserDto.getUserId());
    }

    @Test
    void getOrdersIdIn_ShouldReturnOrdersWithUsers_WhenIdsExist() {
        Set<UUID> ids = Set.of(TEST_ORDER_UUID);
        when(orderRepository.findAllByIdIn(ids)).thenReturn(List.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserDto);

        List<OrderWithUserResponse> results = orderService.getOrdersIdIn(ids);

        assertFalse(results.isEmpty());
        assertEquals(testUserDto, results.get(0).getUser());

        verify(orderRepository, times(1)).findAllByIdIn(ids);
        verify(userClient, times(1)).getUserById(testOrderDto.getUserId());
    }

    @Test
    void getOrdersIdIn_ShouldFilterByCurrentUser_WhenUserIsNotAdmin() {
        Set<UUID> ids = Set.of(TEST_ORDER_UUID);
        when(orderAuthorizationService.getCurrentUserIdIfNotAdmin()).thenReturn(Optional.of(TEST_USER_ID));
        when(orderRepository.findAllByIdInAndUserId(ids, TEST_USER_ID)).thenReturn(List.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserDto);

        List<OrderWithUserResponse> results = orderService.getOrdersIdIn(ids);

        assertFalse(results.isEmpty());
        verify(orderRepository).findAllByIdInAndUserId(ids, TEST_USER_ID);
        verify(orderRepository, never()).findAllByIdIn(any());
    }

    @Test
    void findByStatusIn_ShouldReturnOrdersWithUsers_WhenStatusesExist() {
        Set<OrderStatus> statuses = Set.of(OrderStatus.CREATED);
        when(orderRepository.findByStatusIn(statuses)).thenReturn(List.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserDto);

        List<OrderWithUserResponse> results = orderService.findByStatusIn(statuses);

        assertFalse(results.isEmpty());
        assertEquals(testUserDto, results.get(0).getUser());

        verify(orderRepository, times(1)).findByStatusIn(statuses);
        verify(userClient, times(1)).getUserById(testOrderDto.getUserId());
    }

    @Test
    void findByStatusIn_ShouldFilterByCurrentUser_WhenUserIsNotAdmin() {
        Set<OrderStatus> statuses = Set.of(OrderStatus.CREATED);
        when(orderAuthorizationService.getCurrentUserIdIfNotAdmin()).thenReturn(Optional.of(TEST_USER_ID));
        when(orderRepository.findByStatusInAndUserId(statuses, TEST_USER_ID)).thenReturn(List.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserDto);

        orderService.findByStatusIn(statuses);

        verify(orderRepository).findByStatusInAndUserId(statuses, TEST_USER_ID);
        verify(orderRepository, never()).findByStatusIn(any());
    }

    @Test
    void getAllOrders_ShouldReturnAllOrdersWithUsers_WhenOrdersExist() {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserDto);

        List<OrderWithUserResponse> results = orderService.getAllOrders();

        assertFalse(results.isEmpty());
        assertEquals(testUserDto, results.get(0).getUser());

        verify(orderRepository, times(1)).findAll();
        verify(userClient, times(1)).getUserById(testOrderDto.getUserId());
    }

    @Test
    void getAllOrders_ShouldReturnOnlyCurrentUserOrders_WhenUserIsNotAdmin() {
        when(orderAuthorizationService.getCurrentUserIdIfNotAdmin()).thenReturn(Optional.of(TEST_USER_ID));
        when(orderRepository.findOrdersByUserId(TEST_USER_ID)).thenReturn(List.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserDto);

        orderService.getAllOrders();

        verify(orderRepository).findOrdersByUserId(TEST_USER_ID);
        verify(orderRepository, never()).findAll();
    }

    @Test
    void getAllOrdersNativeWithPagination_ShouldReturnPagedOrderDtos_WhenOrdersExist() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findAllOrdersNative(pageable)).thenReturn(page);

        Page<OrderDto> resultPage = orderService.getAllOrdersNativeWithPagination(0, 10);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());

        verify(orderRepository, times(1)).findAllOrdersNative(pageable);
    }

    @Test
    void getAllOrdersNativeWithPagination_ShouldFilterByCurrentUser_WhenUserIsNotAdmin() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderAuthorizationService.getCurrentUserIdIfNotAdmin()).thenReturn(Optional.of(TEST_USER_ID));
        when(orderRepository.findAllOrdersNativeByUserId(TEST_USER_ID, pageable)).thenReturn(page);

        orderService.getAllOrdersNativeWithPagination(0, 10);

        verify(orderRepository).findAllOrdersNativeByUserId(TEST_USER_ID, pageable);
        verify(orderRepository, never()).findAllOrdersNative(any());
    }
}
