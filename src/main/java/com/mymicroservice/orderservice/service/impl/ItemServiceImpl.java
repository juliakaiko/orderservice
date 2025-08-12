package com.mymicroservice.orderservice.service.impl;

import com.mymicroservice.orderservice.dto.ItemDto;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.mapper.ItemMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemDto createItem(ItemDto ItemDto) {
        Item item = ItemMapper.INSTANSE.toEntity(ItemDto);
        log.info("createItem(): {}",item);
        item = itemRepository.save(item);
        return ItemMapper.INSTANSE.toDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getItemById(Long itemId) {
        Optional<Item> itemFromDb = Optional.ofNullable(itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item wasn't found with id " + itemId)));
        log.info("getItemsById(): {}",itemId);
        return ItemMapper.INSTANSE.toDto(itemFromDb.get());
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, ItemDto itemDetails) {
        Optional<Item> itemFromDb = Optional.ofNullable(itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item wasn't found with id " + itemId)));
        Item item = itemFromDb.get();
        item.setName(itemDetails.getName());
        item.setPrice(itemDetails.getPrice());
        log.info("updateItem(): {}",item);
        itemRepository.save(item);
        return ItemMapper.INSTANSE.toDto(item);
    }

    @Override
    @Transactional
    public ItemDto deleteItem(Long itemId) {
        Optional<Item> itemFromDb = Optional.ofNullable(itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item wasn't found with id " + itemId)));
        itemRepository.deleteById(itemId);
        log.info("deleteItem(): {}",itemId);
        return ItemMapper.INSTANSE.toDto(itemFromDb.get());
    }

    @Override
    @Transactional(readOnly = true)
    public List <ItemDto> getItemsIdIn(Set<Long> ids) {
        List<Item> itemList = itemRepository.findAllByIdIn(ids);
        log.info("getItemsIdIn()");
        return itemList.stream().map(ItemMapper.INSTANSE::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List <ItemDto> getAllItems() {
        List<Item> itemList = itemRepository.findAll();
        log.info("getAllItems()");
        return itemList.stream().map(ItemMapper.INSTANSE::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemDto> getAllItemsNativeWithPagination(Integer page, Integer size) {
        var pageable = PageRequest.of(page,size, Sort.by("id"));
        Page<Item> itemList = itemRepository.findAllItemsNative(pageable);
        log.info("findAllItemsNativeWithPagination()");
        return itemList.map(ItemMapper.INSTANSE::toDto);
    }
}
