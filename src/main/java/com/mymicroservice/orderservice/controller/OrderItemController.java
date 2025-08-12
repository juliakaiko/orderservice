package com.mymicroservice.orderservice.controller;

import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.service.OrderItemService;
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
@RequestMapping("/order-service/api/order-items")
@Tag(name="OrderItemController")
@Slf4j
@Validated // for @NotEmpty
public class OrderItemController {

    private final OrderItemService orderItemService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderItemById (@PathVariable("id") Long id) {
        log.info("Request to find the OrderItem by id: {}", id);
        OrderItemDto orderItemDto = orderItemService.getOrderItemById(id);
        return ObjectUtils.isEmpty(orderItemDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(orderItemDto);
    }

    @PostMapping("/")
    public ResponseEntity<?> createOrderItem (@RequestBody @Valid OrderItemDto orderItemDto){
        log.info("Request to create a new OrderItem: {}", orderItemDto);
        OrderItemDto savedOrderItemDto =  orderItemService.createOrderItem(orderItemDto);
        return ObjectUtils.isEmpty(savedOrderItemDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(savedOrderItemDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity <?> updateOrderItem (@PathVariable("id") Long id,
                                               @RequestBody @Valid OrderItemDto orderItemDto){
        log.info("Request to update the OrderItem: {}", orderItemDto);

        OrderItemDto updatedOrderItemDto =  orderItemService.updateOrderItem(id, orderItemDto);

        return ObjectUtils.isEmpty(updatedOrderItemDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(updatedOrderItemDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity <?> deleteOrderItem (@PathVariable("id") Long id){
        log.info("Request to delete the OrderItem by id: {}", id);

        OrderItemDto deletedOrderItemDto = orderItemService.deleteOrderItem(id);

        return ObjectUtils.isEmpty(deletedOrderItemDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(deletedOrderItemDto);
    }

    @GetMapping("/find-by-ids")
    public ResponseEntity<List<OrderItemDto>> getOrderItemsIdIn(@RequestParam @NotEmpty Set<Long> ids) {
        log.info("Request to find OrderItems by IDs: {}", ids);
        List<OrderItemDto> orderItemDtos = orderItemService.getOrderItemsIdIn(ids);
        return ResponseEntity.ok(orderItemDtos);
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderItemDto>> getAllOrderItems() {
        log.info("Request to find all OrderItems");
        return ResponseEntity.ok(orderItemService.getAllOrderItems());
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<OrderItemDto>> getAllOrderItemsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Request to find all OrderItems with pagination");
        return ResponseEntity.ok(orderItemService.getAllOrderItemsNativeWithPagination(page, size));
    }
}
