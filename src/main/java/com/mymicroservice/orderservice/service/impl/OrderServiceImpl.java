package com.mymicroservice.orderservice.service.impl;

import com.mymicroservice.orderservice.client.UserClient;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserDto;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderAlreadyPaidException;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.security.OrderAuthorizationService;
import com.mymicroservice.orderservice.service.OrderService;
import com.mymicroservice.orderservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final UserClient userClient;
    private final OrderAuthorizationService orderAuthorizationService;

    @Override
    @Transactional
    public OrderWithUserResponse createOrder(OrderDto orderDto) {
        orderAuthorizationService.verifyCanCreateOrderForUser(orderDto.getUserId());

        UserDto userDtoFromUserService = userClient.getUserById(orderDto.getUserId());
        log.info("Validated user {} before order creation", userDtoFromUserService.getUserId());

        Order order = OrderMapper.INSTANCE.toEntity(orderDto);
        order.setCreationDate(LocalDateTime.now().withNano(0));
        order.setStatus(OrderStatus.CREATED);
        log.info("createOrder(): {}", order);

        if (order.getOrderItems() != null) {
            for (OrderItem orderItem : order.getOrderItems()) {
                Item fullItem = itemRepository.findById(orderItem.getItem().getId())
                        .orElseThrow(() -> new ItemNotFoundException("Item not found: " + orderItem.getItem().getId()));
                orderItem.setItem(fullItem);
                orderItem.setOrder(order);
            }
        }

        order = orderRepository.save(order);
        OrderDto orderDtoFromDb = OrderMapper.INSTANCE.toDto(order);

        OrderEventDto event = createOrderEvent(order);
        outboxService.saveOutboxEvent(event, createEventType(order));

        return new OrderWithUserResponse(orderDtoFromDb, userDtoFromUserService);
    }

    private OrderEventDto createOrderEvent(Order order) {
        OrderEventDto orderEvent = new OrderEventDto();
        orderEvent.setOrderId(order.getId().toString());
        orderEvent.setUserId(order.getUserId().toString());

        Set<OrderItem> orderItems = order.getOrderItems() == null
                ? Collections.emptySet()
                : order.getOrderItems();

        BigDecimal paymentAmount = orderItems.stream()
                .map(item -> item.getItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        orderEvent.setPaymentAmount(paymentAmount);
        return orderEvent;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWithUserResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId));
        orderAuthorizationService.verifyOrderOwnership(order.getUserId());

        log.info("getOrdersById(): {}", orderId);
        OrderDto orderDtoFromDb = OrderMapper.INSTANCE.toDto(order);
        UserDto userDtoFromUserService = userClient.getUserById(orderDtoFromDb.getUserId());
        return new OrderWithUserResponse(orderDtoFromDb, userDtoFromUserService);
    }

    @Override
    @Transactional
    public OrderWithUserResponse updateOrder(UUID orderId, OrderDto orderDetails) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId));

        orderAuthorizationService.verifyOrderOwnership(order.getUserId());

        if (order.getStatus().equals(OrderStatus.PAID)) {
            throw new OrderAlreadyPaidException("Order with id " + orderId + " is already PAID and cannot be modified");
        }

        order.setUserId(orderDetails.getUserId());
        if (orderDetails.getStatus() == null) {
            order.setStatus(order.getStatus());
        } else {
            order.setStatus(orderDetails.getStatus());
        }

        if (orderDetails.getOrderItems() != null && !orderDetails.getOrderItems().isEmpty()) {
            order.getOrderItems().clear();

            for (OrderItemDto orderItemDto : orderDetails.getOrderItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);

                Item item = itemRepository.findById(orderItemDto.getItemId())
                        .orElseThrow(() -> new ItemNotFoundException("Item not found: " + orderItemDto.getItemId()));
                orderItem.setItem(item);
                orderItem.setQuantity(orderItemDto.getQuantity());
                order.getOrderItems().add(orderItem);
            }
        }

        log.info("updateOrder(): {}", order);
        Order updatedOrder = orderRepository.save(order);
        OrderDto orderDtoFromDb = OrderMapper.INSTANCE.toDto(updatedOrder);
        UserDto userDtoFromUserService = userClient.getUserById(orderDtoFromDb.getUserId());

        return new OrderWithUserResponse(orderDtoFromDb, userDtoFromUserService);
    }

    @Override
    @Transactional
    public OrderDto deleteOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId));

        orderAuthorizationService.verifyOrderOwnership(order.getUserId());

        orderRepository.deleteById(orderId);
        log.info("deleteOrder(): {}", order);
        return OrderMapper.INSTANCE.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderWithUserResponse> getOrdersByUserEmail(String email) {
        orderAuthorizationService.verifyCanAccessUserEmail(email);

        UserDto userFromUserService = userClient.getUserByEmail(email);
        log.info("getOrdersByUserEmail: {}", email);
        List<Order> orderList = orderRepository.findOrdersByUserId(userFromUserService.getUserId());
        return orderList.stream()
                .map(order -> new OrderWithUserResponse(
                        OrderMapper.INSTANCE.toDto(order),
                        userFromUserService))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderWithUserResponse> getOrdersIdIn(Set<UUID> ids) {
        List<Order> orderList = orderAuthorizationService.getCurrentUserIdIfNotAdmin()
                .map(userId -> orderRepository.findAllByIdInAndUserId(ids, userId))
                .orElseGet(() -> orderRepository.findAllByIdIn(ids));

        log.info("getOrdersIdIn()");
        return toOrderWithUserResponseList(orderList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderWithUserResponse> findByStatusIn(Set<OrderStatus> statuses) {
        List<Order> orderList = orderAuthorizationService.getCurrentUserIdIfNotAdmin()
                .map(userId -> orderRepository.findByStatusInAndUserId(statuses, userId))
                .orElseGet(() -> orderRepository.findByStatusIn(statuses));

        log.info("findByStatusIn()");
        return toOrderWithUserResponseList(orderList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderWithUserResponse> getAllOrders() {
        List<Order> orderList = orderAuthorizationService.getCurrentUserIdIfNotAdmin()
                .map(orderRepository::findOrdersByUserId)
                .orElseGet(orderRepository::findAll);

        log.info("getAllOrders()");
        return toOrderWithUserResponseList(orderList);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> getAllOrdersNativeWithPagination(Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<Order> orderList = orderAuthorizationService.getCurrentUserIdIfNotAdmin()
                .map(userId -> orderRepository.findAllOrdersNativeByUserId(userId, pageable))
                .orElseGet(() -> orderRepository.findAllOrdersNative(pageable));

        log.info("findAllOrdersNativeWithPagination()");
        return orderList.map(OrderMapper.INSTANCE::toDto);
    }

    private String createEventType(Order order) {
        String aggregateName = order.getClass().getSimpleName().toUpperCase();
        String orderStatus = order.getStatus().toString();
        return aggregateName + "_" + orderStatus;
    }

    private List<OrderWithUserResponse> toOrderWithUserResponseList(List<Order> orderList) {
        return orderList.stream().map(order -> {
            OrderDto orderDto = OrderMapper.INSTANCE.toDto(order);
            UserDto userDto = userClient.getUserById(orderDto.getUserId());
            return new OrderWithUserResponse(orderDto, userDto);
        }).toList();
    }
}
