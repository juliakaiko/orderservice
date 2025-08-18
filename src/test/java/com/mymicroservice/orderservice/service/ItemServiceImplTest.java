package com.mymicroservice.orderservice.service;

import com.mymicroservice.orderservice.dto.ItemDto;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.mapper.ItemMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.service.impl.ItemServiceImpl;
import com.mymicroservice.orderservice.util.ItemGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ItemServiceImplTest {

    @InjectMocks
    private ItemServiceImpl itemService;

    @Mock
    private ItemRepository itemRepository;

    private final static Long TEST_ITEM_ID = 1L;
    private Item testItem;
    private ItemDto testItemDto;

    @BeforeEach
    void setUp() {
        testItem = ItemGenerator.generateItem();
        testItem.setId(TEST_ITEM_ID);

        testItemDto = ItemMapper.INSTANSE.toDto(testItem);
    }

    @Test
    void createNewOrderItem_returnsItemDto() {
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        ItemDto result = itemService.createItem(testItemDto);

        assertNotNull(result);
        assertEquals(testItemDto, result);

        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    void getItemById_whenIdExists_thenReturnsItemDto() {
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));

        ItemDto result = itemService.getItemById(TEST_ITEM_ID);

        assertNotNull(result);
        assertEquals(testItemDto, result);

        verify(itemRepository, times(1)).findById(TEST_ITEM_ID);
    }

    @Test
    void getItemById_whenIdNotExist_thenThrowsException() {
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.getItemById(TEST_ITEM_ID));

        verify(itemRepository, times(1)).findById(TEST_ITEM_ID);
    }

    @Test
    void updateItem_whenIdExists_thenReturnsItemDto() {
        ItemDto updatedItemDto = new ItemDto();
        updatedItemDto.setName("updated_item");
        updatedItemDto.setPrice(BigDecimal.valueOf(101));

        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        ItemDto result = itemService.updateItem(TEST_ITEM_ID, updatedItemDto);

        assertNotNull(result);
        assertEquals(result.getName(), updatedItemDto.getName());
        assertEquals(result.getPrice(), updatedItemDto.getPrice());

        verify(itemRepository, times(1)).findById(TEST_ITEM_ID);
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    void updateItem_whenIdNotExist_thenThrowsException() {
        ItemDto updatedItemDto = new ItemDto();
        updatedItemDto.setName("updated_item");
        updatedItemDto.setPrice(BigDecimal.valueOf(101));

        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.updateItem(TEST_ITEM_ID, updatedItemDto));

        verify(itemRepository, times(1)).findById(TEST_ITEM_ID);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void deleteItem_whenIdExists_thenDeletesAndReturnsItemDto() {
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));

        ItemDto result = itemService.deleteItem(TEST_ITEM_ID);

        assertNotNull(result);
        assertEquals(testItemDto, result);

        verify(itemRepository, times(1)).findById(TEST_ITEM_ID);
        verify(itemRepository, times(1)).deleteById(TEST_ITEM_ID);
    }

    @Test
    void deleteItem_whenIdNotExist_thenThrowsException() {
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.deleteItem(TEST_ITEM_ID));

        verify(itemRepository, times(1)).findById(TEST_ITEM_ID);
        verify(itemRepository, never()).deleteById(anyLong());
    }

    @Test
    void getItemsIdIn_whenIdsExists_thenReturnsItemDtos() {
        Set<Long> ids = Set.of(TEST_ITEM_ID);
        when(itemRepository.findAllByIdIn(ids)).thenReturn(List.of(testItem));

        List<ItemDto> results = itemService.getItemsIdIn(ids);

        assertFalse(results.isEmpty());
        assertEquals(testItemDto, results.get(0));

        verify(itemRepository, times(1)).findAllByIdIn(ids);
    }

    @Test
    void getAllItems_thenReturnsAllItemDto() {
        when(itemRepository.findAll()).thenReturn(List.of(testItem));

        List<ItemDto> results = itemService.getAllItems();

        assertFalse(results.isEmpty());
        assertEquals(testItemDto, results.get(0));

        verify(itemRepository, times(1)).findAll();
    }

    @Test
    void getAllItemsNativeWithPagination_ReturnsPagedItemDtos() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<Item> page = new PageImpl<>(List.of(testItem));
        when(itemRepository.findAllItemsNative(pageable)).thenReturn(page);

        Page<ItemDto> resultPage = itemService.getAllItemsNativeWithPagination(0, 10);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());

        verify(itemRepository, times(1)).findAllItemsNative(pageable);
    }
    
}
