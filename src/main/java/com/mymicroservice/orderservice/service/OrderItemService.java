package com.mymicroservice.orderservice.service;

import com.mymicroservice.orderservice.dto.OrderItemDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

public interface OrderItemService {

     OrderItemDto createOrderItem(OrderItemDto orderItemDto);
     OrderItemDto getOrderItemById(Long orderItemId);
     OrderItemDto updateOrderItem(Long orderItemId, OrderItemDto orderItemDetails);
     OrderItemDto deleteOrderItem(Long orderItemId);
     List<OrderItemDto> getOrderItemsIdIn(Set<Long> ids);
     List<OrderItemDto> getAllOrderItems();
     Page<OrderItemDto> getAllOrderItemsNativeWithPagination(Integer page, Integer size);
}
