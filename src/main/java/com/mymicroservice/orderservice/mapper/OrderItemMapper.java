package com.mymicroservice.orderservice.mapper;

import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.model.OrderItem;
import lombok.NonNull;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OrderItemMapper {

    OrderItemMapper INSTANCE = Mappers.getMapper(OrderItemMapper.class);

    @Mapping(target = "id", source = "orderItem.id")
    @Mapping(target = "orderId", source = "orderItem.order.id") //ID from Order
    @Mapping(target = "itemId", source = "orderItem.item.id") //ID from Item
    @Mapping(target = "quantity", source = "orderItem.quantity")
    OrderItemDto toDto(OrderItem orderItem);

    /**
     * Converts {@link OrderItemDto} back to {@link OrderItem} entity.
     * <p>
     * Implements <b>reverse mapping</b> relative to {@code OrderItem -> OrderItemDto} conversion.
     * </p>
     *
     * @param orderItemDto DTO object to convert (cannot be {@code null})
     * @return corresponding {@link OrderItem} entity
     */
    @InheritInverseConfiguration
    OrderItem toEntity (@NonNull OrderItemDto orderItemDto);
}
