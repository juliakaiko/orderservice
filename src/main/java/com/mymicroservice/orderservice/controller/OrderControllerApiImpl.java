package com.mymicroservice.orderservice.controller;

import com.mymicroservice.orderservice.api.OrdersApi;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Реализация REST API заказов, сгенерированного из OpenAPI-спецификации (api.yaml).
 * Интерфейс {@link OrdersApi} создаётся openapi-generator-maven-plugin на этапе сборки.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderControllerApiImpl implements OrdersApi {

    private final OrderService orderService;

    @Override
    public ResponseEntity<OrderWithUserResponse> getOrderById(UUID id) {
        log.info("Request to find the Order by id: {}", id);
        OrderWithUserResponse orderWithUserResponse = orderService.getOrderById(id);
        return ObjectUtils.isEmpty(orderWithUserResponse)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(orderWithUserResponse);
    }

    @Override
    public ResponseEntity<OrderWithUserResponse> createOrder(OrderDto orderDto) {
        log.info("Request to create a new Order: {}", orderDto);
        OrderWithUserResponse orderWithUserResponse = orderService.createOrder(orderDto);
        return ObjectUtils.isEmpty(orderWithUserResponse)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(orderWithUserResponse);
    }

    @Override
    public ResponseEntity<OrderWithUserResponse> updateOrder(UUID id, OrderDto orderDto) {
        log.info("Request to update the Order: {}", orderDto);
        OrderWithUserResponse orderWithUserResponse = orderService.updateOrder(id, orderDto);
        return ObjectUtils.isEmpty(orderWithUserResponse)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(orderWithUserResponse);
    }

    @Override
    public ResponseEntity<OrderDto> deleteOrder(UUID id) {
        log.info("Request to delete the Order by id: {}", id);
        OrderDto deletedOrderDto = orderService.deleteOrder(id);
        return ObjectUtils.isEmpty(deletedOrderDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(deletedOrderDto);
    }

    @Override
    public ResponseEntity<List<OrderWithUserResponse>> getOrdersByUserEmail(String email) {
        log.info("Request to find all Orders of the User with email: {}", email);
        return ResponseEntity.ok(orderService.getOrdersByUserEmail(email));
    }

    @Override
    public ResponseEntity<List<OrderWithUserResponse>> getOrdersIdIn(List<UUID> ids) {
        log.info("Request to find Orders by IDs: {}", ids);
        Set<UUID> idSet = Set.copyOf(ids);
        return ResponseEntity.ok(orderService.getOrdersIdIn(idSet));
    }

    @Override
    public ResponseEntity<List<OrderWithUserResponse>> getByStatusIn(List<OrderStatus> statuses) {
        log.info("Request to find Orders by statuses: {}", statuses);
        Set<OrderStatus> statusSet = statuses.stream().collect(Collectors.toSet());
        return ResponseEntity.ok(orderService.findByStatusIn(statusSet));
    }

    @Override
    public ResponseEntity<List<OrderWithUserResponse>> getAllOrders() {
        log.info("Request to find all Orders");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ResponseEntity<Page> getAllOrdersWithPagination(Integer page, Integer size) {
        log.info("Request to find all Orders with pagination");
        return ResponseEntity.ok(orderService.getAllOrdersNativeWithPagination(page, size));
    }
}
