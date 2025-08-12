package com.mymicroservice.orderservice.service;

import com.mymicroservice.orderservice.client.UserClient;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserResponse;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.service.impl.OrderServiceImpl;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.UserResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserClient userClient;

    private final static Long TEST_ORDER_ID = 1L;
    private Order testOrder;
    private OrderDto testOrderDto;
    private UserResponse testUserResponse;
    private OrderWithUserResponse testOrderWithUserResponse;

    @BeforeEach
    void setUp() {
        testOrder = OrderGenerator.generateOrder();
        testOrder.setId(TEST_ORDER_ID);

        testOrderDto = OrderMapper.INSTANSE.toDto(testOrder);

        testUserResponse = UserResponseGenerator.generateUserResponse();

        testOrderWithUserResponse = new OrderWithUserResponse(testOrderDto, testUserResponse);
    }

    @Test
    void createNewOrder_ReturnsOrderWithUserResponse() {
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserResponse);

        OrderWithUserResponse result = orderService.createOrder(testOrderDto);

        assertNotNull(result);
        assertEquals(testOrderWithUserResponse, result);
        assertEquals(testOrderWithUserResponse.getOrder(), result.getOrder());
        assertEquals(testOrderWithUserResponse.getUser(), result.getUser());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(userClient, times(1)).getUserById(TEST_ORDER_ID);
    }

    @Test
    void getOrderById_whenIdExists_thenReturnsOrderWithUserResponse() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserResponse);

        OrderWithUserResponse result = orderService.getOrderById(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(testOrderDto, result.getOrder());
        assertEquals(testUserResponse, result.getUser());

        verify(orderRepository, times(1)).findById(TEST_ORDER_ID);
        verify(userClient, times(1)).getUserById(testOrderDto.getUserId());
    }

    @Test
    void getOrderById_whenIdNotExist_thenThrowsException() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(TEST_ORDER_ID));

        verify(orderRepository, times(1)).findById(TEST_ORDER_ID);
        verifyNoInteractions(userClient);
    }

    @Test
    void updateOrder_whenIdExists_thenReturnsUpdatedOrderWithUserResponse() {
        OrderDto updatedOrderDto = new OrderDto();
        updatedOrderDto.setUserId(testOrderDto.getUserId());
        updatedOrderDto.setStatus(OrderStatus.SHIPPED);
        updatedOrderDto.setCreationDate(LocalDate.of(2023, 3, 3));

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(userClient.getUserById(updatedOrderDto.getUserId())).thenReturn(testUserResponse);

        OrderWithUserResponse result = orderService.updateOrder(TEST_ORDER_ID, updatedOrderDto);

        assertNotNull(result);
        assertEquals(updatedOrderDto.getStatus(), result.getOrder().getStatus());

        verify(orderRepository, times(1)).findById(TEST_ORDER_ID);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(userClient, times(1)).getUserById(updatedOrderDto.getUserId());
    }

    @Test
    void updateOrder_whenIdNotExist_thenThrowsException() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.updateOrder(TEST_ORDER_ID, testOrderDto));

        verify(orderRepository, times(1)).findById(TEST_ORDER_ID);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(userClient);
    }

    @Test
    void deleteOrder_whenIdExists_thenDeletesAndReturnsOrderDto() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.deleteOrder(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(testOrderDto, result);

        verify(orderRepository, times(1)).findById(TEST_ORDER_ID);
        verify(orderRepository, times(1)).deleteById(TEST_ORDER_ID);
    }

    @Test
    void deleteOrder_whenIdNotExist_thenThrowsException() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrder(TEST_ORDER_ID));

        verify(orderRepository, times(1)).findById(TEST_ORDER_ID);
        verify(orderRepository, never()).deleteById(anyLong());
    }

    @Test
    void getOrdersByUserEmail_whenEmailExists_thenReturnsOrdersWithUser() {
        when(userClient.getUserByEmail(anyString())).thenReturn(testUserResponse);
        when(orderRepository.findOrdersByUserId(testUserResponse.getUserId()))
                .thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> results = orderService.getOrdersByUserEmail("test@example.com");

        assertFalse(results.isEmpty());
        assertEquals(testUserResponse, results.get(0).getUser());

        verify(userClient, times(1)).getUserByEmail(anyString());
        verify(orderRepository, times(1)).findOrdersByUserId(testUserResponse.getUserId());
    }

    @Test
    void getOrdersIdIn_whenIdsExists_thenReturnsOrdersWithUsers() {
        Set<Long> ids = Set.of(TEST_ORDER_ID);
        when(orderRepository.findAllByIdIn(ids)).thenReturn(List.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserResponse);

        List<OrderWithUserResponse> results = orderService.getOrdersIdIn(ids);

        assertFalse(results.isEmpty());
        assertEquals(testUserResponse, results.get(0).getUser());

        verify(orderRepository, times(1)).findAllByIdIn(ids);
        verify(userClient, times(1)).getUserById(testOrderDto.getUserId());
    }

    @Test
    void findByStatusIn_whenStatusesExists_thenReturnsOrdersWithUsers() {
        Set<OrderStatus> statuses = Set.of(OrderStatus.NEW);
        when(orderRepository.findByStatusIn(statuses)).thenReturn(List.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserResponse);

        List<OrderWithUserResponse> results = orderService.findByStatusIn(statuses);

        assertFalse(results.isEmpty());
        assertEquals(testUserResponse, results.get(0).getUser());

        verify(orderRepository, times(1)).findByStatusIn(statuses);
        verify(userClient, times(1)).getUserById(testOrderDto.getUserId());
    }

    @Test
    void getAllOrders_thenReturnsAllOrdersWithUsers() {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));
        when(userClient.getUserById(testOrderDto.getUserId())).thenReturn(testUserResponse);

        List<OrderWithUserResponse> results = orderService.getAllOrders();

        assertFalse(results.isEmpty());
        assertEquals(testUserResponse, results.get(0).getUser());

        verify(orderRepository, times(1)).findAll();
        verify(userClient, times(1)).getUserById(testOrderDto.getUserId());
    }

    @Test
    void getAllOrdersNativeWithPagination_ReturnsPagedOrderDtos() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findAllOrdersNative(pageable)).thenReturn(page);

        Page<OrderDto> resultPage = orderService.getAllOrdersNativeWithPagination(0, 10);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());

        verify(orderRepository, times(1)).findAllOrdersNative(pageable);
    }

}
