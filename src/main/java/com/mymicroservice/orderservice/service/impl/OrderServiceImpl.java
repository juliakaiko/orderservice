package com.mymicroservice.orderservice.service.impl;

import com.mymicroservice.orderservice.client.UserClient;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserResponse;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;

    @Override
    @Transactional
    public OrderWithUserResponse createOrder(OrderDto orderDto) {
        Order order = OrderMapper.INSTANSE.toEntity(orderDto);
        log.info("createOrder(): {}",order);
        order.setCreationDate(LocalDate.now());
        order = orderRepository.save(order);
        OrderDto orderDtoFromDb = OrderMapper.INSTANSE.toDto(order);
        UserResponse userDtoFromUserService = userClient.getUserById(orderDto.getUserId());
        return new OrderWithUserResponse (orderDtoFromDb, userDtoFromUserService);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWithUserResponse getOrderById(Long orderId) {
        Optional<Order> orderFromDb = Optional.ofNullable(orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId)));
        log.info("getOrdersById(): {}",orderId);
        OrderDto orderDtoFromDb=OrderMapper.INSTANSE.toDto(orderFromDb.get());
        UserResponse userDtoFromUserService = userClient.getUserById(orderDtoFromDb.getUserId());
        return new OrderWithUserResponse (orderDtoFromDb, userDtoFromUserService);
    }

    @Override
    @Transactional
    public OrderWithUserResponse updateOrder(Long orderId, OrderDto orderDetails) {
        Optional<Order> orderFromDb = Optional.ofNullable(orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId)));
        Order order = orderFromDb.get();
        order.setUserId(orderDetails.getUserId());
        order.setStatus(orderDetails.getStatus());
        order.setCreationDate(orderDetails.getCreationDate());
        log.info("updateOrder(): {}",order);
        orderRepository.save(order);
        OrderDto orderDtoFromDb=OrderMapper.INSTANSE.toDto(order);
        UserResponse userDtoFromUserService = userClient.getUserById(orderDtoFromDb.getUserId());
        return new OrderWithUserResponse (orderDtoFromDb, userDtoFromUserService);
    }
    
    @Override
    @Transactional
    public OrderDto deleteOrder(Long orderId) {
        Optional<Order> orderFromDb = Optional.ofNullable(orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId)));
        orderRepository.deleteById(orderId);
        log.info("deleteOrder(): {}",orderFromDb);
        return OrderMapper.INSTANSE.toDto(orderFromDb.get());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderWithUserResponse> getOrdersByUserEmail(String email) {
        UserResponse userFromUserService = userClient.getUserByEmail(email);
        log.info("getOrdersByUserEmail: {}",email);
        Long userId = userFromUserService.getUserId();
        List <Order> orderList = orderRepository.findOrdersByUserId(userId);
        return orderList.stream().map(order -> {
            OrderDto orderDto = OrderMapper.INSTANSE.toDto(order);
            return new OrderWithUserResponse(orderDto, userFromUserService);
        }).toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderWithUserResponse> getOrdersIdIn(Set<Long> ids) {
        List <Order> orderList = orderRepository.findAllByIdIn(ids);
        log.info("getOrdersIdIn()");
        return toOrderWithUserResponseList(orderList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderWithUserResponse> findByStatusIn(Set<OrderStatus> statuses) {
        List <Order> orderList = orderRepository.findByStatusIn(statuses);
        log.info("findByStatusIn()");
        return toOrderWithUserResponseList(orderList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderWithUserResponse> getAllOrders() {
        List <Order> orderList = orderRepository.findAll();
        log.info("getAllOrders()");
        return toOrderWithUserResponseList(orderList);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> getAllOrdersNativeWithPagination(Integer page, Integer size) {
        var pageable  = PageRequest.of(page,size, Sort.by("id"));
        Page<Order> orderList = orderRepository.findAllOrdersNative(pageable);
        log.info("findAllOrdersNativeWithPagination()");
        return orderList.map(OrderMapper.INSTANSE::toDto);
    }

    private List<OrderWithUserResponse> toOrderWithUserResponseList (List <Order> orderList){
        return orderList.stream().map(order -> {
            OrderDto orderDto = OrderMapper.INSTANSE.toDto(order);
            UserResponse userResponse =  userClient.getUserById(orderDto.getUserId());
            return new OrderWithUserResponse(orderDto, userResponse);
        }).toList();
    }
}
