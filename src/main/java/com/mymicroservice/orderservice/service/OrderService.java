package com.mymicroservice.orderservice.service;

import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserResponse;
import com.mymicroservice.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

public interface OrderService {

    OrderWithUserResponse createOrder(OrderDto orderDto);
    OrderWithUserResponse getOrderById(Long orderId);
    OrderWithUserResponse updateOrder(Long orderId, OrderDto orderDetails);
    OrderDto deleteOrder(Long orderId);
    List<OrderWithUserResponse> getOrdersByUserEmail(String email);
    List<OrderWithUserResponse> getOrdersIdIn(Set<Long> ids);
    List<OrderWithUserResponse> findByStatusIn(Set<OrderStatus> statuses);
    List<OrderWithUserResponse> getAllOrders();
    Page<OrderDto> getAllOrdersNativeWithPagination(Integer page, Integer size);

}
