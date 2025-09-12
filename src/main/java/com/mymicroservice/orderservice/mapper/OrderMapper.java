package com.mymicroservice.orderservice.mapper;

import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import lombok.NonNull;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(uses = OrderItemMapper.class)
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "userId", source = "order.userId")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "creationDate", source = "order.creationDate")
    @Mapping(target = "orderItems", source = "order.orderItems", qualifiedByName = "orderItemsToDtos")
    OrderDto toDto(Order order);

    /**
     * Converts {@link OrderDto} back to {@link Order} entity.
     * <p>
     * Implements <b>reverse mapping</b> relative to {@code Order -> OrderDto} conversion.
     * </p>
     *
     * @param orderDto DTO object to convert (cannot be {@code null})
     * @return corresponding {@link Order} entity
     */
    @InheritInverseConfiguration
    @Mapping(target = "orderItems", source = "orderDto.orderItems", qualifiedByName = "orderItemsToEntities")
    Order toEntity (@NonNull OrderDto orderDto);

    @Named("orderItemsToDtos")
    default Set<OrderItemDto> orderItemsToDtos(Set<OrderItem> orderItems) {
        if (orderItems == null) {
            return Set.of();
        }
        return orderItems.stream()
                .map(OrderItemMapper.INSTANCE::toDto)
                .collect(Collectors.toSet());
    }

    @Named("orderItemsToEntities")
    default Set<OrderItem> orderItemsToEntities(Set<OrderItemDto> orderItemDtos) {
        if (orderItemDtos == null) {
            return Set.of();
        }
        return orderItemDtos.stream()
                .map(OrderItemMapper.INSTANCE::toEntity)
                .collect(Collectors.toSet());
    }
}
