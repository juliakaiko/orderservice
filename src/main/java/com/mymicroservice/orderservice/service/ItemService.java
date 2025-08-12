package com.mymicroservice.orderservice.service;

import com.mymicroservice.orderservice.dto.ItemDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

public interface ItemService {

    ItemDto createItem(ItemDto ItemDto);
    ItemDto getItemById(Long itemId);
    ItemDto updateItem(Long itemId, ItemDto itemDetails);
    ItemDto deleteItem(Long itemId);
    List<ItemDto> getItemsIdIn(Set<Long> ids);
    List<ItemDto> getAllItems();
    Page<ItemDto> getAllItemsNativeWithPagination(Integer page, Integer size);
}
