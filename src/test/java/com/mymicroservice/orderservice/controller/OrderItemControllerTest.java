package com.mymicroservice.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.exception.OrderItemNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderItemMapper;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.service.OrderItemService;
import com.mymicroservice.orderservice.util.OrderItemGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderItemController.class)
@Slf4j
public class OrderItemControllerTest {

    @MockBean
    private OrderItemService orderItemService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final static Long ORDER_ITEM_ID = 1L;
    private OrderItem testOrderItem;
    private OrderItemDto testOrderItemDto;


    @BeforeEach
    void setUp() {
        testOrderItem = OrderItemGenerator.generateOrderItem();
        testOrderItem.setId(ORDER_ITEM_ID);

        testOrderItemDto = OrderItemMapper.INSTANSE.toDto(testOrderItem);
    }

    @Test
    public void getOrderItemById_ShouldReturnOrderItemDto() throws Exception {
        log.info("▶ Running test: getOrderItemById_ShouldReturnOrderItemDto(), ORDER_ITEM_ID={}", ORDER_ITEM_ID);
        when(orderItemService.getOrderItemById(ORDER_ITEM_ID)).thenReturn(testOrderItemDto);

        mockMvc.perform(get("/order-service/api/order-items/{id}", ORDER_ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ITEM_ID));

        verify(orderItemService).getOrderItemById(ORDER_ITEM_ID);
    }

    @Test
    public void getOrderItemById_ShouldReturnNotFound() throws Exception {
        log.info("▶ Running test: getOrderItemById_ShouldReturnNotFound(), ORDER_ITEM_ID={}", ORDER_ITEM_ID);
        when(orderItemService.getOrderItemById(ORDER_ITEM_ID)).thenReturn(null);

        mockMvc.perform(get("/order-service/api/order-items/{id}", ORDER_ITEM_ID))
                .andExpect(status().isNotFound());

        verify(orderItemService).getOrderItemById(ORDER_ITEM_ID);
    }

    @Test
    public void createOrderItem_ShouldReturnCreatedOrderItemDto() throws Exception {
        log.info("▶ Running test: createOrderItem_ShouldReturnCreatedOrderItemDto, OrderItem={}", testOrderItemDto);
        when(orderItemService.createOrderItem(any(OrderItemDto.class))).thenReturn(testOrderItemDto);

        mockMvc.perform(post("/order-service/api/order-items/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderItemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ITEM_ID));

        verify(orderItemService).createOrderItem(any(OrderItemDto.class));
    }

    @Test
    public void updateOrderItem_ShouldReturnUpdatedOrderItemDto() throws Exception {
        OrderItemDto updatedDto = OrderItemMapper.INSTANSE.toDto(OrderItemGenerator.generateOrderItem());
        updatedDto.setId(1L);
        updatedDto.setItemId(1L);
        updatedDto.setItemId(1L);
        updatedDto.setQuantity(11l);
        log.info("▶ Running test: updateOrderItem_ShouldReturnUpdatedOrderItemDto, UPDATED_ORDER_ITEM={}", updatedDto);

        when(orderItemService.updateOrderItem(ORDER_ITEM_ID, updatedDto)).thenReturn(updatedDto);

        mockMvc.perform(put("/order-service/api/order-items/{id}", ORDER_ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ITEM_ID))
                .andExpect(jsonPath("$.quantity").value(updatedDto.getQuantity()));

        verify(orderItemService).updateOrderItem(ORDER_ITEM_ID, updatedDto);
    }

    @Test
    public void updateOrderItem_ShouldReturnNotFound() throws Exception {
        OrderItemDto updatedDto = OrderItemMapper.INSTANSE.toDto(OrderItemGenerator.generateOrderItem());
        updatedDto.setId(1L);
        updatedDto.setItemId(1L);
        updatedDto.setItemId(1L);
        updatedDto.setQuantity(11l);
        log.info("▶ Running test: updateOrderItem_ShouldReturnNotFound, UPDATED_ORDER_ITEM={}", updatedDto);

        when(orderItemService.updateOrderItem(ORDER_ITEM_ID, updatedDto))
                .thenThrow(new OrderItemNotFoundException("OrderItem wasn't found with id " + ORDER_ITEM_ID));

        mockMvc.perform(put("/order-service/api/order-items/{id}", ORDER_ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isNotFound());

        verify(orderItemService).updateOrderItem(ORDER_ITEM_ID, updatedDto);
    }

    @Test
    public void deleteOrderItem_ShouldReturnDeletedOrderItemDto() throws Exception {
        log.info("▶ Running test: deleteOrderItem_ShouldReturnDeletedOrderItemDto, ORDER_ITEM_ID={}", ORDER_ITEM_ID);
        when(orderItemService.deleteOrderItem(ORDER_ITEM_ID)).thenReturn(testOrderItemDto);

        mockMvc.perform(delete("/order-service/api/order-items/{id}", ORDER_ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ITEM_ID))
                .andExpect(jsonPath("$.quantity").value(testOrderItemDto.getQuantity()));

        verify(orderItemService).deleteOrderItem(ORDER_ITEM_ID);
    }

    @Test
    public void deleteOrderItem_ShouldReturnNotFound() throws Exception {
        log.info("▶ Running test: deleteOrderItem_ShouldReturnNotFound, ORDER_ITEM_ID={}", ORDER_ITEM_ID);
        when(orderItemService.deleteOrderItem(ORDER_ITEM_ID)).thenReturn(null);

        mockMvc.perform(delete("/order-service/api/order-items/{id}", ORDER_ITEM_ID))
                .andExpect(status().isNotFound());

        verify(orderItemService).deleteOrderItem(ORDER_ITEM_ID);
    }

    @Test
    public void getOrderItemsIdIn_ShouldReturnOrderItemsForGivenIds() throws Exception {
        Set<Long> ids = Set.of(ORDER_ITEM_ID);
        log.info("▶ Running test: getOrderItemsIdIn_ShouldReturnOrderItemsForGivenIds, ids={}", ids);
        when(orderItemService.getOrderItemsIdIn(ids)).thenReturn(List.of(testOrderItemDto));

        mockMvc.perform(get("/order-service/api/order-items/find-by-ids")
                        .param("ids", ORDER_ITEM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ORDER_ITEM_ID));

        verify(orderItemService).getOrderItemsIdIn(ids);
    }

    @Test
    public void getOrderItemsIdIn_ShouldReturnEmptyListWhenNoMatches() throws Exception {
        Set<Long> ids = Set.of(999L);
        log.info("▶ Running test: getOrderItemsIdIn_ShouldReturnEmptyListWhenNoMatches, ids={}", ids);
        when(orderItemService.getOrderItemsIdIn(ids)).thenReturn(List.of());

        mockMvc.perform(get("/order-service/api/order-items/find-by-ids")
                        .param("ids", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(orderItemService).getOrderItemsIdIn(ids);
    }

    @Test
    public void getAllOrderItems_ShouldReturnAllOrderItems() throws Exception {
        log.info("▶ Running test: getAllOrderItems_ShouldReturnAllOrderItems");
        when(orderItemService.getAllOrderItems()).thenReturn(List.of(testOrderItemDto));

        mockMvc.perform(get("/order-service/api/order-items/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ORDER_ITEM_ID));

        verify(orderItemService).getAllOrderItems();
    }

    @Test
    public void getAllOrderItemsWithPagination_ShouldReturnPaginatedOrderItems() throws Exception {
        log.info("▶ Running test: getAllOrderItemsWithPagination_ShouldReturnPaginatedOrderItems, page=0, size=10");
        Page<OrderItemDto> page = new PageImpl<>(List.of(testOrderItemDto));
        when(orderItemService.getAllOrderItemsNativeWithPagination(0, 10)).thenReturn(page);

        mockMvc.perform(get("/order-service/api/order-items/paginated")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ORDER_ITEM_ID));

        verify(orderItemService).getAllOrderItemsNativeWithPagination(0, 10);
    }
}
