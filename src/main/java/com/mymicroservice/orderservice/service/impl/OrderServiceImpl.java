package com.mymicroservice.orderservice.service.impl;

import com.mymicroservice.orderservice.client.UserClient;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserDto;
import org.mymicroservices.common.events.OrderEventDto;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderAlreadyPaidException;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.kafka.OrderEventProducer;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserClient userClient;
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    public OrderWithUserResponse createOrder(OrderDto orderDto) {
        Order order = OrderMapper.INSTANCE.toEntity(orderDto);
        log.info("createOrder(): {}", order);
        order.setCreationDate(LocalDate.now());
        order.setStatus(OrderStatus.CREATED);

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

        log.info("BEFORE calling userClient.getUserById({})", orderDto.getUserId());
        UserDto userDtoFromUserService = userClient.getUserById(orderDto.getUserId());
        log.info("BEFORE calling userClient.getUserById({})", userDtoFromUserService.getUserId());

        // Send event to PaymentService
        OrderEventDto event = createOrderEvent(order);
        // sending with a callback, the status update will be performed after successful sending
        orderEventProducer.sendCreateOrder(event, () -> {
            updateOrderStatus(orderDtoFromDb.getId(), OrderStatus.PROCESSING);
        });
        return new OrderWithUserResponse(orderDtoFromDb, userDtoFromUserService);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId));
        order.setStatus(status);
        orderRepository.save(order);
        log.info("Order with id {} was updated with status {}", orderId, status);
    }

    private OrderEventDto createOrderEvent(Order order) {
        OrderEventDto orderEvent = new OrderEventDto();
        orderEvent.setOrderId(order.getId().toString());
        orderEvent.setUserId(order.getUserId().toString());

        BigDecimal paymentAmount = order.getOrderItems().stream()
                .map(item -> item.getItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        orderEvent.setPaymentAmount(paymentAmount);

        return orderEvent;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWithUserResponse getOrderById(Long orderId) {
        Optional<Order> orderFromDb = Optional.ofNullable(orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId)));
        log.info("getOrdersById(): {}",orderId);
        OrderDto orderDtoFromDb=OrderMapper.INSTANCE.toDto(orderFromDb.get());
        UserDto userDtoFromUserService = userClient.getUserById(orderDtoFromDb.getUserId());
        return new OrderWithUserResponse (orderDtoFromDb, userDtoFromUserService);
    }

    @Override
    @Transactional
    public OrderWithUserResponse updateOrder(Long orderId, OrderDto orderDetails) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId));

        if (order.getStatus().equals(OrderStatus.PAID)) {
            throw new OrderAlreadyPaidException("Order with id " + orderId + " is already PAID and cannot be modified");
        }
        order.setUserId(orderDetails.getUserId());
        if (orderDetails.getStatus() == null)
            order.setStatus(order.getStatus());
        else
            order.setStatus(orderDetails.getStatus());

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

        // Send event to PaymentService
        OrderEventDto event = createOrderEvent(order);
        // sending with a callback, the status update will be performed after successful sending
        orderEventProducer.sendCreateOrder(event, () -> {
            updateOrderStatus(orderDtoFromDb.getId(), OrderStatus.PROCESSING);
        });
        return new OrderWithUserResponse(orderDtoFromDb, userDtoFromUserService);
    }
    
    @Override
    @Transactional
    public OrderDto deleteOrder(Long orderId) {
        Optional<Order> orderFromDb = Optional.ofNullable(orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order wasn't found with id " + orderId)));
        orderRepository.deleteById(orderId);
        log.info("deleteOrder(): {}",orderFromDb);
        return OrderMapper.INSTANCE.toDto(orderFromDb.get());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderWithUserResponse> getOrdersByUserEmail(String email) {
        UserDto userFromUserService = userClient.getUserByEmail(email);
        log.info("getOrdersByUserEmail: {}",email);
        Long userId = userFromUserService.getUserId();
        List <Order> orderList = orderRepository.findOrdersByUserId(userId);
        return orderList.stream().map(order -> {
            OrderDto orderDto = OrderMapper.INSTANCE.toDto(order);
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
        return orderList.map(OrderMapper.INSTANCE::toDto);
    }

    private List<OrderWithUserResponse> toOrderWithUserResponseList (List <Order> orderList){
        return orderList.stream().map(order -> {
            OrderDto orderDto = OrderMapper.INSTANCE.toDto(order);
            UserDto userDto =  userClient.getUserById(orderDto.getUserId());
            return new OrderWithUserResponse(orderDto, userDto);
        }).toList();
    }
}
