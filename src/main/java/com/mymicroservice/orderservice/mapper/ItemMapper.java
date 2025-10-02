package com.mymicroservice.orderservice.mapper;

import com.mymicroservice.orderservice.dto.ItemDto;
import com.mymicroservice.orderservice.model.Item;
import lombok.NonNull;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ItemMapper {

    ItemMapper INSTANCE = Mappers.getMapper(ItemMapper.class);

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "name", source = "item.name") //ID from Order
    @Mapping(target = "price", source = "item.price") //ID from Item
    ItemDto toDto(Item item);

    /**
     * Converts {@link ItemDto} back to {@link Item} entity.
     * <p>
     * Implements <b>reverse mapping</b> relative to {@code Item -> ItemDto} conversion.
     * </p>
     *
     * @param itemDto DTO object to convert (cannot be {@code null})
     * @return corresponding {@link Item} entity
     */
    @InheritInverseConfiguration
    Item toEntity (@NonNull ItemDto itemDto);
}
