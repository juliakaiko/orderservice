package com.mymicroservice.orderservice.mapper;

import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.util.OrderGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class OrderMapperTest {

    @Test
    public void itemToDto_whenOk_thenMapFieldsCorrectly() {
        Order order = OrderGenerator.generateOrder();
        OrderDto orderDto = OrderMapper.INSTANCE.toDto(order);
        assertEquals(order.getId(), orderDto.getId());
        assertEquals(order.getUserId(), orderDto.getUserId());
        assertEquals(order.getStatus(), orderDto.getStatus());
        assertEquals(order.getCreationDate(), orderDto.getCreationDate());
    }

    @Test
    public void orderDtoToEntity_whenOk_thenMapFieldsCorrectly() {
        Order order = OrderGenerator.generateOrder();
        OrderDto orderDto = OrderMapper.INSTANCE.toDto(order);
        order = OrderMapper.INSTANCE.toEntity(orderDto);
        assertEquals(orderDto.getId(), order.getId());
        assertEquals(orderDto.getUserId(), order.getUserId());
        assertEquals(orderDto.getStatus(), order.getStatus());
        assertEquals(orderDto.getCreationDate(), order.getCreationDate());
    }
}
