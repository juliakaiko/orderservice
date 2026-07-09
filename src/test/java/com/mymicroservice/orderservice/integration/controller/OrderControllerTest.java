package com.mymicroservice.orderservice.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.orderservice.controller.OrderController;
import com.mymicroservice.orderservice.config.SecurityConfig;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderItemDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserDto;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import com.mymicroservice.orderservice.service.OrderService;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.UserGenerator;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_ORDER_UUID;
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
@ActiveProfiles("test")
@Slf4j
public class OrderControllerTest {

    @MockBean
    private OrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Order testOrder;
    private OrderDto testOrderDto;
    private UserDto testUserDto;
    private OrderWithUserResponse testOrderWithUserResponse;

    @BeforeEach
    void setUp() {
        testOrder = OrderGenerator.generateOrder();
        testOrder.setId(TEST_ORDER_UUID);

        testOrderDto = OrderMapper.INSTANCE.toDto(testOrder);
        testOrderDto.setOrderItems(Set.of(new OrderItemDto(1L,TEST_ORDER_UUID,2L,5L)));

        testUserDto = UserGenerator.generateUserResponse();

        testOrderWithUserResponse = new OrderWithUserResponse(testOrderDto, testUserDto);
    }

    @Test
    public void getOrderById_ShouldReturnOrderWithUserResponse() throws Exception {
        log.info("▶ Running test: getOrderById_ShouldReturnOrderWithUserResponse, TEST_ORDER_UUID={}", TEST_ORDER_UUID);
        when(orderService.getOrderById(TEST_ORDER_UUID)).thenReturn(testOrderWithUserResponse);

        mockMvc.perform(get("/api/orders/{id}", TEST_ORDER_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(TEST_ORDER_UUID.toString()));

        verify(orderService).getOrderById(TEST_ORDER_UUID);
    }

    @Test
    public void getOrderById_ShouldReturnNotFound() throws Exception {
        log.info("▶ Running test: getOrderById_ShouldReturnNotFound, TEST_ORDER_UUID={}", TEST_ORDER_UUID);
        when(orderService.getOrderById(TEST_ORDER_UUID)).thenReturn(null);

        mockMvc.perform(get("/api/orders/{id}", TEST_ORDER_UUID))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(TEST_ORDER_UUID);
    }

    @Test
    public void createOrder_ShouldReturnCreatedOrderWithUserResponse() throws Exception {
        log.info("▶ Running test: createOrder_ShouldReturnCreatedOrderWithUserResponse, ORDER={}", testOrderDto);
        when(orderService.createOrder(any(OrderDto.class))).thenReturn(testOrderWithUserResponse);

        mockMvc.perform(post("/api/orders/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(TEST_ORDER_UUID.toString()));

        verify(orderService).createOrder(any(OrderDto.class));
    }

    @Test
    public void updateOrder_ShouldReturnUpdatedOrderWithUserResponse() throws Exception {
        OrderDto updatedDto = OrderMapper.INSTANCE.toDto(OrderGenerator.generateOrder());
        updatedDto.setId(TEST_ORDER_UUID);
        updatedDto.setUserId(10L);
        updatedDto.setStatus(OrderStatus.CANCELLED);
        updatedDto.setOrderItems(Set.of(
                new OrderItemDto(100L, TEST_ORDER_UUID, 2L, 3L)
        ));
        log.info("▶ Running test: updateOrder_ShouldReturnUpdatedOrderWithUserResponse, UPDATED_ORDER={}", updatedDto);

        OrderWithUserResponse updatedResponse = new OrderWithUserResponse(updatedDto, testUserDto);

        when(orderService.updateOrder(eq(TEST_ORDER_UUID), any(OrderDto.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/orders/{id}", TEST_ORDER_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(TEST_ORDER_UUID.toString()))
                .andExpect(jsonPath("$.order.status").value(OrderStatus.CANCELLED.name()));

        verify(orderService).updateOrder(eq(TEST_ORDER_UUID), any(OrderDto.class));
    }

    @Test
    public void updateOrder_ShouldReturnNotFound() throws Exception {
        OrderDto updatedDto = OrderMapper.INSTANCE.toDto(OrderGenerator.generateOrder());
        updatedDto.setId(TEST_ORDER_UUID);
        updatedDto.setUserId(10L);
        updatedDto.setStatus(OrderStatus.CANCELLED);
        updatedDto.setOrderItems(Set.of(
                new OrderItemDto(100L, TEST_ORDER_UUID, 2L, 3L)
        ));
        log.info("▶ Running test: updateOrder_ShouldReturnNotFound, UPDATED_ORDER={}", updatedDto);

        when(orderService.updateOrder(eq(TEST_ORDER_UUID), any(OrderDto.class)))
                .thenThrow(new OrderNotFoundException("Order wasn't found with id " + TEST_ORDER_UUID));

        mockMvc.perform(put("/api/orders/{id}", TEST_ORDER_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isNotFound());

        verify(orderService).updateOrder(eq(TEST_ORDER_UUID), any(OrderDto.class));
    }

    @Test
    public void deleteOrder_ShouldReturnDeletedOrderDto() throws Exception {
        log.info("▶ Running test: deleteOrder_ShouldReturnDeletedOrderDto, TEST_ORDER_UUID={}", TEST_ORDER_UUID);
        when(orderService.deleteOrder(TEST_ORDER_UUID)).thenReturn(testOrderDto);

        mockMvc.perform(delete("/api/orders/{id}", TEST_ORDER_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_ORDER_UUID.toString()))
                .andExpect(jsonPath("$.status").value(OrderStatus.CREATED.name()));

        verify(orderService).deleteOrder(TEST_ORDER_UUID);
    }

    @Test
    public void deleteOrder_ShouldReturnNotFound() throws Exception {
        log.info("▶ Running test: deleteOrder_ShouldReturnNotFound, TEST_ORDER_UUID={}", TEST_ORDER_UUID);
        when(orderService.deleteOrder(TEST_ORDER_UUID)).thenReturn(null);

        mockMvc.perform(delete("/api/orders/{id}", TEST_ORDER_UUID))
                .andExpect(status().isNotFound());

        verify(orderService).deleteOrder(TEST_ORDER_UUID);
    }

    @Test
    public void getOrdersByUserEmail_ShouldReturnListOfOrders() throws Exception {
        String email = "test@example.com";
        log.info("▶ Running test: getOrdersByUserEmail_ShouldReturnListOfOrders, email={}", email);
        when(orderService.getOrdersByUserEmail(email)).thenReturn(List.of(testOrderWithUserResponse));

        mockMvc.perform(get("/api/orders/by-email")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order.id").value(TEST_ORDER_UUID.toString()));

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
        Set<UUID> ids = Set.of(TEST_ORDER_UUID);
        log.info("▶ Running test: getOrdersIdIn_ShouldReturnOrdersForGivenIds, ids={}", ids);
        when(orderService.getOrdersIdIn(ids)).thenReturn(List.of(testOrderWithUserResponse));

        mockMvc.perform(get("/api/orders/find-by-ids")
                        .param("ids", TEST_ORDER_UUID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order.id").value(TEST_ORDER_UUID.toString()));

        verify(orderService).getOrdersIdIn(ids);
    }

    @Test
    public void getOrdersIdIn_ShouldReturnEmptyListWhenNoMatches() throws Exception {
        Set<UUID> ids = Set.of(TEST_ORDER_UUID);
        log.info("▶ Running test: getOrdersIdIn_ShouldReturnEmptyListWhenNoMatches, ids={}", ids);
        when(orderService.getOrdersIdIn(ids)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/find-by-ids")
                        .param("ids", TEST_ORDER_UUID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(orderService).getOrdersIdIn(ids);
    }

    @Test
    public void getByStatusIn_ShouldReturnOrdersWithGivenStatuses() throws Exception {
        Set<OrderStatus> statuses = Set.of(OrderStatus.CREATED);
        log.info("▶ Running test: getByStatusIn_ShouldReturnOrdersWithGivenStatuses, statuses={}", statuses);
        when(orderService.findByStatusIn(statuses)).thenReturn(List.of(testOrderWithUserResponse));

        mockMvc.perform(get("/api/orders/find-by-statuses")
                        .param("statuses", OrderStatus.CREATED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order.id").value(TEST_ORDER_UUID.toString()));

        verify(orderService).findByStatusIn(statuses);
    }

    @Test
    public void getAllOrders_ShouldReturnAllOrders() throws Exception {
        log.info("▶ Running test: getAllOrders_ShouldReturnAllOrders");
        when(orderService.getAllOrders()).thenReturn(List.of(testOrderWithUserResponse));

        mockMvc.perform(get("/api/orders/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order.id").value(TEST_ORDER_UUID.toString()));

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
                .andExpect(jsonPath("$.content[0].id").value(TEST_ORDER_UUID.toString()));

        verify(orderService).getAllOrdersNativeWithPagination(0, 10);
    }
}
