package com.mymicroservice.orderservice.controller;

import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.service.OrderService;
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
@RequestMapping("/api/orders")
@Tag(name="OrderController")
@Slf4j
@Validated // for @NotEmpty
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById (@PathVariable("id") Long id) {
        log.info("Request to find the Order by id: {}", id);
        OrderWithUserResponse orderWithUserResponse = orderService.getOrderById(id);
        return ObjectUtils.isEmpty(orderWithUserResponse)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(orderWithUserResponse);
    }

    @PostMapping({"", "/"})
    public ResponseEntity<?> createOrder (@RequestBody @Valid OrderDto orderDto){
        log.info("Request to create a new Order: {}", orderDto);
        OrderWithUserResponse orderWithUserResponse =  orderService.createOrder(orderDto);
        return ObjectUtils.isEmpty(orderWithUserResponse)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(orderWithUserResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity <?> updateOrder (@PathVariable("id") Long id,
                                           @RequestBody @Valid OrderDto orderDto){
        log.info("Request to update the Order: {}", orderDto);

        OrderWithUserResponse orderWithUserResponse =  orderService.updateOrder(id, orderDto);

        return ObjectUtils.isEmpty(orderWithUserResponse)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(orderWithUserResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity <?> deleteOrder (@PathVariable("id") Long id){
        log.info("Request to delete the Order by id: {}", id);

        OrderDto deletedOrderDto = orderService.deleteOrder(id);

        return ObjectUtils.isEmpty(deletedOrderDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(deletedOrderDto);
    }

    @GetMapping("/by-email")
    public ResponseEntity<List<OrderWithUserResponse>> getOrdersByUserEmail(@RequestParam("email") String email) {
        log.info("Request to find all Orders of the User with email: {}", email);
        return ResponseEntity.ok(orderService.getOrdersByUserEmail(email));
    }

    @GetMapping("/find-by-ids")
    public ResponseEntity<List<OrderWithUserResponse>> getOrdersIdIn(@RequestParam @NotEmpty Set<Long> ids) {
        log.info("Request to find Orders by IDs: {}", ids);
        List<OrderWithUserResponse> orderWithUserResponses = orderService.getOrdersIdIn(ids);
        return ResponseEntity.ok(orderWithUserResponses);
    }

    @GetMapping("/find-by-statuses")
    public ResponseEntity<List<OrderWithUserResponse>> getByStatusIn(@RequestParam @NotEmpty Set<OrderStatus> statuses) {
        log.info("Request to find Orders by statuses: {}", statuses);
        List<OrderWithUserResponse> orderWithUserResponses = orderService.findByStatusIn(statuses);
        return ResponseEntity.ok(orderWithUserResponses);
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderWithUserResponse>> getAllOrders() {
        log.info("Request to find all Orders");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<OrderDto>> getAllOrdersWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Request to find all Orders with pagination");
        return ResponseEntity.ok(orderService.getAllOrdersNativeWithPagination(page, size));
    }
}
