package com.mymicroservice.orderservice.mapper;

import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.model.Order;
import lombok.NonNull;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OrderMapper {

    OrderMapper INSTANSE = Mappers.getMapper(OrderMapper.class);

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "userId", source = "order.userId")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "creationDate", source = "order.creationDate")
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
    Order toEntity (@NonNull OrderDto orderDto);
}
