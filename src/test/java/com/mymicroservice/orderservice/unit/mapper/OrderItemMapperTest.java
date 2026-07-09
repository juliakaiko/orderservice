package com.mymicroservice.orderservice.unit.mapper;

import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.mapper.OrderItemMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.util.ItemGenerator;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.OrderItemGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemMapperTest {

    @Test
    void toDto_ShouldMapFieldsCorrectly_WhenOrderItemIsValid() {
        OrderItem orderItem = OrderItemGenerator.generateOrderItem();
        Order order = OrderGenerator.generateOrder();
        Item item = ItemGenerator.generateItem();
        orderItem.setOrder(order);
        orderItem.setItem(item);

        OrderItemDto orderItemDto = OrderItemMapper.INSTANCE.toDto(orderItem);

        assertEquals(orderItem.getId(), orderItemDto.getId());
        assertEquals(orderItem.getOrder().getId(), orderItemDto.getOrderId());
        assertEquals(orderItem.getItem().getId(), orderItemDto.getItemId());
        assertEquals(orderItem.getQuantity(), orderItemDto.getQuantity());
    }

    @Test
    void toEntity_ShouldMapFieldsCorrectly_WhenOrderItemDtoIsValid() {
        OrderItem orderItem = OrderItemGenerator.generateOrderItem();
        Order order = OrderGenerator.generateOrder();
        Item item = ItemGenerator.generateItem();
        orderItem.setOrder(order);
        orderItem.setItem(item);

        OrderItemDto orderItemDto = OrderItemMapper.INSTANCE.toDto(orderItem);
        OrderItem mapped = OrderItemMapper.INSTANCE.toEntity(orderItemDto);

        assertEquals(orderItemDto.getId(), mapped.getId());
        assertEquals(orderItemDto.getOrderId(), mapped.getOrder().getId());
        assertEquals(orderItemDto.getItemId(), mapped.getItem().getId());
        assertEquals(orderItemDto.getQuantity(), mapped.getQuantity());
    }
}
