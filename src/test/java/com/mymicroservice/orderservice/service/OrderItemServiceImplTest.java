package com.mymicroservice.orderservice.service;

import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderItemMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.service.impl.OrderItemServiceImpl;
import com.mymicroservice.orderservice.util.ItemGenerator;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.OrderItemGenerator;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderItemServiceImplTest {

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderRepository orderRepository;

    private final static Long TEST_ORDER_ITEM_ID = 1L;
    private OrderItem testOrderItem;
    private OrderItemDto testOrderItemDto;

    @BeforeEach
    void setUp() {
        testOrderItem = OrderItemGenerator.generateOrderItem();
        testOrderItem.setId(TEST_ORDER_ITEM_ID);

        testOrderItemDto = OrderItemMapper.INSTANCE.toDto(testOrderItem);
    }

    @Test
    void createNewOrderItem_returnsOrderItemDto() {
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        OrderItemDto result = orderItemService.createOrderItem(testOrderItemDto);

        assertNotNull(result);
        assertEquals(testOrderItemDto, result);

        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    void getOrderItemById_whenIdExists_thenReturnsOrderItemDto() {
        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(testOrderItem));

        OrderItemDto result = orderItemService.getOrderItemById(TEST_ORDER_ITEM_ID);

        assertNotNull(result);
        assertEquals(testOrderItemDto, result);

        verify(orderItemRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
    }

    @Test
    void getOrderItemById_whenIdNotExist_thenThrowsException() {
        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(OrderItemNotFoundException.class, () -> orderItemService.getOrderItemById(TEST_ORDER_ITEM_ID));

        verify(orderItemRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
    }

    @Test
    void updateOrderItem_whenIdExists_thenReturnsOrderItemDto() {
        OrderItemDto updatedOrderItemDto = new OrderItemDto();
        updatedOrderItemDto.setQuantity(101l);
        updatedOrderItemDto.setOrderId(1l);
        updatedOrderItemDto.setItemId(1l);

        Order order = OrderGenerator.generateOrder();
        order.setId(1l);

        Item item = ItemGenerator.generateItem();
        item.setId(1l);

        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(order));
        when(itemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(item));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        OrderItemDto result = orderItemService.updateOrderItem(TEST_ORDER_ITEM_ID, updatedOrderItemDto);

        assertNotNull(result);
        assertEquals(result.getQuantity(), updatedOrderItemDto.getQuantity());
        assertEquals(result.getOrderId(), updatedOrderItemDto.getOrderId());
        assertEquals(result.getItemId(), updatedOrderItemDto.getItemId());

        verify(orderItemRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
        verify(orderRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
        verify(itemRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    void updateOrderItem_whenIdNotExist_thenThrowsException() {
        OrderItemDto updatedOrderItemDto = new OrderItemDto();
        updatedOrderItemDto.setQuantity(101l);
        updatedOrderItemDto.setOrderId(1l);
        updatedOrderItemDto.setItemId(1l);

        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(OrderItemNotFoundException.class, () -> orderItemService.updateOrderItem(TEST_ORDER_ITEM_ID, updatedOrderItemDto));

        verify(orderItemRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void updateOrderItem_whenOrderIdNotExist_thenThrowsException() {
        OrderItemDto updatedOrderItemDto = new OrderItemDto();
        updatedOrderItemDto.setQuantity(101l);
        updatedOrderItemDto.setOrderId(1l);
        updatedOrderItemDto.setItemId(1l);

        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(updatedOrderItemDto.getOrderId())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderItemService.updateOrderItem(TEST_ORDER_ITEM_ID, updatedOrderItemDto));

        verify(orderItemRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
        verify(orderRepository, times(1)).findById(updatedOrderItemDto.getOrderId());
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void updateOrderItem_whenItemIdNotExist_thenThrowsException() {
        OrderItemDto updatedOrderItemDto = new OrderItemDto();
        updatedOrderItemDto.setQuantity(101l);
        updatedOrderItemDto.setOrderId(1l);
        updatedOrderItemDto.setItemId(1l);

        Order order = OrderGenerator.generateOrder();
        order.setId(1l);

        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(updatedOrderItemDto.getOrderId())).thenReturn(Optional.of(order));
        when(itemRepository.findById(updatedOrderItemDto.getItemId())).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> orderItemService.updateOrderItem(TEST_ORDER_ITEM_ID, updatedOrderItemDto));

        verify(orderItemRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
        verify(orderRepository, times(1)).findById(updatedOrderItemDto.getOrderId());
        verify(itemRepository, times(1)).findById(updatedOrderItemDto.getItemId());
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void deleteOrderItem_whenIdExists_thenDeletesAndReturnsOrderItemDto() {
        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(testOrderItem));

        OrderItemDto result = orderItemService.deleteOrderItem(TEST_ORDER_ITEM_ID);

        assertNotNull(result);
        assertEquals(testOrderItemDto, result);

        verify(orderItemRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
        verify(orderItemRepository, times(1)).deleteById(TEST_ORDER_ITEM_ID);
    }

    @Test
    void deleteOrderItem_whenIdNotExist_thenThrowsException() {
        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(OrderItemNotFoundException.class, () -> orderItemService.deleteOrderItem(TEST_ORDER_ITEM_ID));

        verify(orderItemRepository, times(1)).findById(TEST_ORDER_ITEM_ID);
        verify(orderItemRepository, never()).deleteById(anyLong());
    }

    @Test
    void getOrderItemsIdIn_whenIdsExists_thenReturnsOrderItemDtos() {
        Set<Long> ids = Set.of(TEST_ORDER_ITEM_ID);
        when(orderItemRepository.findAllByIdIn(ids)).thenReturn(List.of(testOrderItem));

        List<OrderItemDto> results = orderItemService.getOrderItemsIdIn(ids);

        assertFalse(results.isEmpty());
        assertEquals(testOrderItemDto, results.get(0));

        verify(orderItemRepository, times(1)).findAllByIdIn(ids);
    }

    @Test
    void getAllOrderItems_thenReturnsAllOrderItemDto() {
        when(orderItemRepository.findAll()).thenReturn(List.of(testOrderItem));

        List<OrderItemDto> results = orderItemService.getAllOrderItems();

        assertFalse(results.isEmpty());
        assertEquals(testOrderItemDto, results.get(0));

        verify(orderItemRepository, times(1)).findAll();
    }

    @Test
    void getAllOrderItemsNativeWithPagination_ReturnsPagedOrderItemDtos() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<OrderItem> page = new PageImpl<>(List.of(testOrderItem));
        when(orderItemRepository.findAllOrderItemsNative(pageable)).thenReturn(page);

        Page<OrderItemDto> resultPage = orderItemService.getAllOrderItemsNativeWithPagination(0, 10);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());

        verify(orderItemRepository, times(1)).findAllOrderItemsNative(pageable);
    }
}
