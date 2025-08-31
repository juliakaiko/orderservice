package com.mymicroservice.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.orderservice.configuration.SecurityConfig;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserResponse;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.service.OrderService;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.UserResponseGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
@WithMockUser(roles = {"ADMIN", "USER"})
@WebMvcTest(OrderController.class)
@Slf4j
public class OrderControllerTest {

    @MockBean
    private OrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final static Long ORDER_ID = 1L;
    private Order testOrder;
    private OrderDto testOrderDto;
    private UserResponse testUserResponse;
    private OrderWithUserResponse testOrderWithUserResponse;

    @BeforeEach
    void setUp() {
        testOrder = OrderGenerator.generateOrder();
        testOrder.setId(ORDER_ID);

        testOrderDto = OrderMapper.INSTANSE.toDto(testOrder);

        testUserResponse = UserResponseGenerator.generateUserResponse();

        testOrderWithUserResponse = new OrderWithUserResponse(testOrderDto, testUserResponse);
    }

    @Test
    public void getOrderById_ShouldReturnOrderWithUserResponse() throws Exception {
        log.info("▶ Running test: getOrderById_ShouldReturnOrderWithUserResponse, ORDER_ID={}", ORDER_ID);
        when(orderService.getOrderById(ORDER_ID)).thenReturn(testOrderWithUserResponse);

        mockMvc.perform(get("/api/orders/{id}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(ORDER_ID));

        verify(orderService).getOrderById(ORDER_ID);
    }

    @Test
    public void getOrderById_ShouldReturnNotFound() throws Exception {
        log.info("▶ Running test: getOrderById_ShouldReturnNotFound, ORDER_ID={}", ORDER_ID);
        when(orderService.getOrderById(ORDER_ID)).thenReturn(null);

        mockMvc.perform(get("/api/orders/{id}", ORDER_ID))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(ORDER_ID);
    }

    @Test
    public void createOrder_ShouldReturnCreatedOrderWithUserResponse() throws Exception {
        log.info("▶ Running test: createOrder_ShouldReturnCreatedOrderWithUserResponse, ORDER={}", testOrderDto);
        when(orderService.createOrder(any(OrderDto.class))).thenReturn(testOrderWithUserResponse);

        mockMvc.perform(post("/api/orders/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(ORDER_ID));

        verify(orderService).createOrder(any(OrderDto.class));
    }

    @Test
    public void updateOrder_ShouldReturnUpdatedOrderWithUserResponse() throws Exception {
        OrderDto updatedDto = OrderMapper.INSTANSE.toDto(OrderGenerator.generateOrder());
        updatedDto.setId(1L);
        updatedDto.setUserId(10L);
        updatedDto.setStatus(OrderStatus.SHIPPED);
        log.info("▶ Running test: updateOrder_ShouldReturnUpdatedOrderWithUserResponse, UPDATED_ORDER={}", updatedDto);

        OrderWithUserResponse updatedResponse = new OrderWithUserResponse(updatedDto, testUserResponse);

        when(orderService.updateOrder(eq(ORDER_ID), any(OrderDto.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/orders/{id}", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(ORDER_ID))
                .andExpect(jsonPath("$.order.status").value(OrderStatus.SHIPPED.name()));

        verify(orderService).updateOrder(eq(ORDER_ID), any(OrderDto.class));
    }

    @Test
    public void updateOrder_ShouldReturnNotFound() throws Exception {
        OrderDto updatedDto = OrderMapper.INSTANSE.toDto(OrderGenerator.generateOrder());
        updatedDto.setId(1L);
        updatedDto.setUserId(10L);
        updatedDto.setStatus(OrderStatus.SHIPPED);
        log.info("▶ Running test: updateOrder_ShouldReturnNotFound, UPDATED_ORDER={}", updatedDto);

        when(orderService.updateOrder(eq(ORDER_ID), any(OrderDto.class)))
                .thenThrow(new OrderNotFoundException("Order wasn't found with id " + ORDER_ID));

        mockMvc.perform(put("/api/orders/{id}", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isNotFound());

        verify(orderService).updateOrder(eq(ORDER_ID), any(OrderDto.class));
    }

    @Test
    public void deleteOrder_ShouldReturnDeletedOrderDto() throws Exception {
        log.info("▶ Running test: deleteOrder_ShouldReturnDeletedOrderDto, ORDER_ID={}", ORDER_ID);
        when(orderService.deleteOrder(ORDER_ID)).thenReturn(testOrderDto);

        mockMvc.perform(delete("/api/orders/{id}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID))
                .andExpect(jsonPath("$.status").value(OrderStatus.NEW.name()));

        verify(orderService).deleteOrder(ORDER_ID);
    }

    @Test
    public void deleteOrder_ShouldReturnNotFound() throws Exception {
        log.info("▶ Running test: deleteOrder_ShouldReturnNotFound, ORDER_ID={}", ORDER_ID);
        when(orderService.deleteOrder(ORDER_ID)).thenReturn(null);

        mockMvc.perform(delete("/api/orders/{id}", ORDER_ID))
                .andExpect(status().isNotFound());

        verify(orderService).deleteOrder(ORDER_ID);
    }

    @Test
    public void getOrdersByUserEmail_ShouldReturnListOfOrders() throws Exception {
        String email = "test@example.com";
        log.info("▶ Running test: getOrdersByUserEmail_ShouldReturnListOfOrders, email={}", email);
        when(orderService.getOrdersByUserEmail(email)).thenReturn(List.of(testOrderWithUserResponse));

        mockMvc.perform(get("/api/orders/by-email")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order.id").value(ORDER_ID));

        verify(orderService).getOrdersByUserEmail(email);
    }

    @Test
    public void getOrdersByUserEmail_ShouldReturnEmptyList() throws Exception {
        String email = "nonexistent@example.com";
        log.info("▶ Running test: getOrdersByUserEmail_ShouldReturnEmptyList, email={}", email);
        when(orderService.getOrdersByUserEmail(email)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/by-email")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(orderService).getOrdersByUserEmail(email);
    }

    @Test
    public void getOrdersIdIn_ShouldReturnOrdersForGivenIds() throws Exception {
        Set<Long> ids = Set.of(ORDER_ID);
        log.info("▶ Running test: getOrdersIdIn_ShouldReturnOrdersForGivenIds, ids={}", ids);
        when(orderService.getOrdersIdIn(ids)).thenReturn(List.of(testOrderWithUserResponse));

        mockMvc.perform(get("/api/orders/find-by-ids")
                        .param("ids", ORDER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order.id").value(ORDER_ID));

        verify(orderService).getOrdersIdIn(ids);
    }

    @Test
    public void getOrdersIdIn_ShouldReturnEmptyListWhenNoMatches() throws Exception {
        Set<Long> ids = Set.of(999L);
        log.info("▶ Running test: getOrdersIdIn_ShouldReturnEmptyListWhenNoMatches, ids={}", ids);
        when(orderService.getOrdersIdIn(ids)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/find-by-ids")
                        .param("ids", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(orderService).getOrdersIdIn(ids);
    }

    @Test
    public void getByStatusIn_ShouldReturnOrdersWithGivenStatuses() throws Exception {
        Set<OrderStatus> statuses = Set.of(OrderStatus.NEW);
        log.info("▶ Running test: getByStatusIn_ShouldReturnOrdersWithGivenStatuses, statuses={}", statuses);
        when(orderService.findByStatusIn(statuses)).thenReturn(List.of(testOrderWithUserResponse));

        mockMvc.perform(get("/api/orders/find-by-statuses")
                        .param("statuses", OrderStatus.NEW.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order.id").value(ORDER_ID));

        verify(orderService).findByStatusIn(statuses);
    }

    @Test
    public void getAllOrders_ShouldReturnAllOrders() throws Exception {
        log.info("▶ Running test: getAllOrders_ShouldReturnAllOrders");
        when(orderService.getAllOrders()).thenReturn(List.of(testOrderWithUserResponse));

        mockMvc.perform(get("/api/orders/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order.id").value(ORDER_ID));

        verify(orderService).getAllOrders();
    }

    @Test
    public void getAllOrdersWithPagination_ShouldReturnPaginatedOrders() throws Exception {
        log.info("▶ Running test: getAllOrdersWithPagination_ShouldReturnPaginatedOrders, page=0, size=10");
        Page<OrderDto> page = new PageImpl<>(List.of(testOrderDto));
        when(orderService.getAllOrdersNativeWithPagination(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/orders/paginated")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ORDER_ID));

        verify(orderService).getAllOrdersNativeWithPagination(0, 10);
    }
}
