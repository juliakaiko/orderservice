package com.mymicroservice.orderservice.mapper;

import com.mymicroservice.orderservice.dto.ItemDto;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.util.ItemGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class ItemMapperTest {

    @Test
    public void itemToDto_whenOk_thenMapFieldsCorrectly() {
        Item item = ItemGenerator.generateItem();
        ItemDto itemDto = ItemMapper.INSTANSE.toDto(item);
        assertEquals(item.getId(), itemDto.getId());
        assertEquals(item.getName(), itemDto.getName());
        assertEquals(item.getPrice(), itemDto.getPrice());
    }

    @Test
    public void itemDtoToEntity_whenOk_thenMapFieldsCorrectly() {
        Item item = ItemGenerator.generateItem();
        ItemDto itemDto = ItemMapper.INSTANSE.toDto(item);
        item = ItemMapper.INSTANSE.toEntity(itemDto);
        assertEquals(itemDto.getId(), item.getId());
        assertEquals(itemDto.getName(), item.getName());
        assertEquals(itemDto.getPrice(), item.getPrice());
    }
}
