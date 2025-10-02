package com.mymicroservice.orderservice.mapper;

import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.util.ItemGenerator;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.OrderItemGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class OrderItemMapperTest {

    @Test
    public void orderItemToDto_whenOk_thenMapFieldsCorrectly() {
        OrderItem orderItem = OrderItemGenerator.generateOrderItem();
        Order order = OrderGenerator.generateOrder();
        Item item = ItemGenerator.generateItem();
        orderItem.setOrder(order);
        orderItem.setItem(item);
        OrderItemDto orderItemDto = OrderItemMapper.INSTANCE.toDto(orderItem);
        assertEquals(orderItem.getId(), orderItemDto.getId());
        assertEquals(orderItem.getOrder().getId(), orderItemDto.getOrderId());
        assertEquals(orderItem.getItem().getId(), orderItemDto.getId());
        assertEquals(orderItem.getQuantity(), orderItemDto.getQuantity());
    }

    @Test
    public void orderItemDtoToEntity_whenOk_thenMapFieldsCorrectly() {
        OrderItem orderItem = OrderItemGenerator.generateOrderItem();
        OrderItemDto orderItemDto = OrderItemMapper.INSTANCE.toDto(orderItem);
        orderItem = OrderItemMapper.INSTANCE.toEntity(orderItemDto);
        assertEquals(orderItemDto.getId(), orderItem.getId());
        assertEquals(orderItemDto.getOrderId(), orderItem.getOrder().getId());
        assertEquals(orderItemDto.getId(), orderItem.getItem().getId());
        assertEquals(orderItemDto.getQuantity(), orderItem.getQuantity());
    }
}
