package com.mymicroservice.orderservice.service.impl;

import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderItemNotFoundException;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderItemMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.repository.ItemRepository;
import com.mymicroservice.orderservice.repository.OrderItemRepository;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderItemDto createOrderItem(OrderItemDto orderItemDto) {
        OrderItem orderItem = OrderItemMapper.INSTANSE.toEntity(orderItemDto);
        log.info("createOrderItem(): {}", orderItem);
        orderItem = orderItemRepository.save(orderItem);
        return OrderItemMapper.INSTANSE.toDto(orderItem);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderItemDto getOrderItemById(Long orderItemId) {
        Optional<OrderItem> orderItemFromDb = Optional.ofNullable(orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new OrderItemNotFoundException("OrderItem wasn't found with id " + orderItemId)));
        log.info("getOrderItemsById(): {}", orderItemId);
        return OrderItemMapper.INSTANSE.toDto(orderItemFromDb.get());
    }

    @Override
    @Transactional
    public OrderItemDto updateOrderItem(Long orderItemId, OrderItemDto orderItemDetails) {
        Optional<OrderItem> orderItemFromDb = Optional.ofNullable(orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new OrderItemNotFoundException("OrderItem wasn't found with id " + orderItemId)));
        OrderItem orderItem = orderItemFromDb.get();
        orderItem.setQuantity(orderItemDetails.getQuantity());

        Optional<Order> orderFromDb = Optional.ofNullable(orderRepository.findById(orderItemDetails.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order to set in OrderItem wasn't found with id " + orderItemDetails.getOrderId())));
        orderItem.setOrder(orderFromDb.get());

        Optional<Item> itemFromDb = Optional.ofNullable(itemRepository.findById(orderItemDetails.getItemId())
                .orElseThrow(() -> new ItemNotFoundException("Item to set in OrderItem wasn't found with id " + orderItemDetails.getItemId())));
        orderItem.setItem(itemFromDb.get());

        log.info("updateOrderItem(): {}", orderItem);
        orderItemRepository.save(orderItem);
        return OrderItemMapper.INSTANSE.toDto(orderItem);
    }

    @Override
    @Transactional
    public OrderItemDto deleteOrderItem(Long orderItemId) {
        Optional<OrderItem> orderItemFromDb = Optional.ofNullable(orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new OrderItemNotFoundException("OrderItem wasn't found with id " + orderItemId)));
        orderItemRepository.deleteById(orderItemId);
        log.info("deleteOrderItem(): {}", orderItemId);
        return OrderItemMapper.INSTANSE.toDto(orderItemFromDb.get());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemDto> getOrderItemsIdIn(Set<Long> ids) {
        List <OrderItem> orderItemList = orderItemRepository.findAllByIdIn(ids);
        log.info("getOrderItemsIdIn()");
        return orderItemList.stream().map(OrderItemMapper.INSTANSE::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemDto> getAllOrderItems() {
        List <OrderItem> orderItemList = orderItemRepository.findAll();
        log.info("getAllOrderItems()");
        return orderItemList.stream().map(OrderItemMapper.INSTANSE::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderItemDto> getAllOrderItemsNativeWithPagination(Integer page, Integer size) {
        var pageable  = PageRequest.of(page,size, Sort.by("id"));
        Page<OrderItem> orderItemList = orderItemRepository.findAllOrderItemsNative(pageable);
        log.info("findAllOrderItemsNativeWithPagination()");
        return orderItemList.map(OrderItemMapper.INSTANSE::toDto);
    }
}
