package com.mymicroservice.orderservice.mapper;

import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.util.OrderGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    public void orderItemsMapping_shouldMapCorrectly() {
        Order order = OrderGenerator.generateOrder();
        Item orderItem = Item.builder().id(1l).name("Item").price(BigDecimal.valueOf(100l)).build();
        OrderItem orderItems = OrderItem.builder().id(1l).order(order).item(orderItem).quantity(3l).build();
        orderItem.setOrderItems(Set.of(orderItems));
        order.setId(1l);
        order.setOrderItems(Set.of(orderItems));

        OrderDto orderDto = OrderMapper.INSTANCE.toDto(order);

        assertEquals(order.getOrderItems().size(), orderDto.getOrderItems().size());

        for (OrderItem item : order.getOrderItems()) {
            boolean match = orderDto.getOrderItems().stream()
                    .anyMatch(dto -> dto.getId().equals(item.getId())
                            && dto.getOrderId().equals(item.getOrder().getId())
                            && dto.getQuantity().equals(item.getQuantity())
                            && dto.getItemId().equals(item.getItem().getId()));
            assertTrue(match, "OrderItem does not match OrderItemDto");
        }

        Order mappedBack = OrderMapper.INSTANCE.toEntity(orderDto);
        assertEquals(order.getOrderItems().size(), mappedBack.getOrderItems().size());

        for (OrderItemDto dto : orderDto.getOrderItems()) {
            boolean match = mappedBack.getOrderItems().stream()
                    .anyMatch(item -> item.getId().equals(dto.getId())
                            && item.getOrder().getId().equals(dto.getOrderId())
                            && item.getQuantity().equals(dto.getQuantity())
                            && item.getItem().getId().equals(dto.getItemId()));
            assertTrue(match, "OrderItemDto does not match OrderItem");
        }
    }

    @Test
    public void orderItemsMapping_shouldHandleNullOrEmpty() {
        Order order = new Order();
        order.setOrderItems(null);

        OrderDto orderDto = OrderMapper.INSTANCE.toDto(order);
        assertNotNull(orderDto.getOrderItems());
        assertTrue(orderDto.getOrderItems().isEmpty(), "Should return an empty Set with null");


        order.setOrderItems(Set.of());
        orderDto = OrderMapper.INSTANCE.toDto(order);
        Order mappedBack = OrderMapper.INSTANCE.toEntity(orderDto);
        assertNotNull(mappedBack.getOrderItems());
        assertTrue(mappedBack.getOrderItems().isEmpty(), "Should return an empty Set with an empty OrderItems");
    }
}
