package com.mymicroservice.orderservice.service;

import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OrderService {

    OrderWithUserResponse createOrder(OrderDto orderDto);

    OrderWithUserResponse getOrderById(UUID orderId);

    OrderWithUserResponse updateOrder(UUID orderId, OrderDto orderDetails);

    OrderDto deleteOrder(UUID orderId);

    List<OrderWithUserResponse> getOrdersByUserEmail(String email);

    List<OrderWithUserResponse> getOrdersIdIn(Set<UUID> ids);

    List<OrderWithUserResponse> findByStatusIn(Set<OrderStatus> statuses);

    List<OrderWithUserResponse> getAllOrders();

    Page<OrderDto> getAllOrdersNativeWithPagination(Integer page, Integer size);

}
