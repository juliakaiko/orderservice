package com.mymicroservice.orderservice.controller;

import com.mymicroservice.orderservice.dto.ItemDto;
import com.mymicroservice.orderservice.service.ItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
@Tag(name="ItemController")
@Slf4j
@Validated // for @NotEmpty
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getItemById (@PathVariable("id") Long id) {
        log.info("Request to find the Item by id: {}", id);
        ItemDto itemDto = itemService.getItemById(id);
        return ObjectUtils.isEmpty(itemDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(itemDto);
    }

    @PostMapping("/")
    public ResponseEntity<?> createItem (@RequestBody @Valid ItemDto itemDto){
        log.info("Request to create a new Item: {}", itemDto);
        ItemDto savedItemDto =  itemService.createItem(itemDto);
        return ObjectUtils.isEmpty(savedItemDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(savedItemDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity <?> updateItem (@PathVariable("id") Long id,
                                          @RequestBody @Valid ItemDto itemDto){
        log.info("Request to update the Item: {}", itemDto);

        ItemDto updatedItemDto =  itemService.updateItem(id, itemDto);

        return ObjectUtils.isEmpty(updatedItemDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(updatedItemDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity <?> deleteItem (@PathVariable("id") Long id){
        log.info("Request to delete the Item by id: {}", id);

        ItemDto deletedItemDto = itemService.deleteItem(id);

        return ObjectUtils.isEmpty(deletedItemDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(deletedItemDto);
    }

    @GetMapping("/find-by-ids")
    public ResponseEntity<List<ItemDto>> getItemsIdIn(@RequestParam @NotEmpty Set<Long> ids) {
        log.info("Request to find Items by IDs: {}", ids);
        List<ItemDto> itemDtos = itemService.getItemsIdIn(ids);
        return ResponseEntity.ok(itemDtos);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ItemDto>> getAllItems() {
        log.info("Request to find all Items");
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<ItemDto>> getAllItemsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Request to find all Items with pagination");
        return ResponseEntity.ok(itemService.getAllItemsNativeWithPagination(page, size));
    }

}
