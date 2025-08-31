package com.mymicroservice.orderservice.service.wiremock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.mymicroservice.orderservice.client.UserClient;
import com.mymicroservice.orderservice.configuration.FeignTestConfig;
import com.mymicroservice.orderservice.dto.OrderDto;
import com.mymicroservice.orderservice.dto.OrderWithUserResponse;
import com.mymicroservice.orderservice.dto.UserResponse;
import com.mymicroservice.orderservice.exception.OrderNotFoundException;
import com.mymicroservice.orderservice.mapper.OrderMapper;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderStatus;
import com.mymicroservice.orderservice.repository.OrderRepository;
import com.mymicroservice.orderservice.service.OrderService;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.UserResponseGenerator;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWireMock(port = 0) // WireMock will work on a random port
@Import({FeignTestConfig.class})
@ActiveProfiles("test")
public class OrderServiceImplWireMockTest {

    @MockBean
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserClient userClient;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("user-service.url", () -> "http://localhost:${wiremock.server.port}");
    }

    private static final Long TEST_ORDER_ID = 1L;
    private static final String TEST_USER_EMAIL = "test@test.by";
    private Order testOrder;
    private OrderDto testOrderDto;
    private UserResponse testUserResponse;
    private OrderWithUserResponse testOrderWithUserResponse;

    @BeforeEach
    void setUp() throws Exception {
        testOrder = OrderGenerator.generateOrder();
        testOrder.setId(TEST_ORDER_ID);
        testOrderDto = OrderMapper.INSTANSE.toDto(testOrder);

        testUserResponse = UserResponseGenerator.generateUserResponse();
        testOrderWithUserResponse = new OrderWithUserResponse(testOrderDto, testUserResponse);

        // Stub for getting user by ID
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/users/1"))
                .withHeader("Authorization", WireMock.equalTo("Bearer test-token"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testUserResponse))));
    }

    @AfterEach
    void tearDown() {
        WireMock.reset(); // Resetting WireMock state after each test
    }

    @Test
    void testCreateNewOrder_ReturnsOrderWithUserResponse()  throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        OrderWithUserResponse result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(testOrderWithUserResponse.getOrder(), result.getOrder());
        assertEquals(testOrderWithUserResponse.getUser(), result.getUser());

        verifyFeignCall("/api/users/1");
    }

    @Test
    void testGetOrderById_whenIdExists_thenReturnsOrderWithUserResponse() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        OrderWithUserResponse result = orderService.getOrderById(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(testOrderWithUserResponse.getOrder(), result.getOrder());
        assertEquals(testOrderWithUserResponse.getUser(), result.getUser());

        verifyFeignCall("/api/users/1");
    }

    @Test
    void testGetOrderById_whenIdNotExist_thenNotFound() {
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(TEST_ORDER_ID);
        });

        WireMock.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/users/1")));
    }

    @Test
    void testUpdateOrder_whenIdExists_thenReturnsOrderWithUserResponse() {
        Order updatedOrder = OrderGenerator.generateOrder();
        updatedOrder.setId(TEST_ORDER_ID);
        updatedOrder.setStatus(OrderStatus.PROCESSING);

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(ArgumentMatchers.any(Order.class))).thenReturn(testOrder);

        OrderDto updateDto = OrderMapper.INSTANSE.toDto(updatedOrder);
        OrderWithUserResponse result = orderService.updateOrder(TEST_ORDER_ID, updateDto);

        assertNotNull(result);
        assertEquals(updateDto.getUserId(), result.getOrder().getUserId());

        verifyFeignCall("/api/users/1");
    }

    //no Feign here
    @Test
    void testDeleteOrder_whenIdExists_thenDeletesAndReturnsOrderDto(){
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.deleteOrder(TEST_ORDER_ID);

        assertNotNull(result);
        assertEquals(testOrderDto.getId(), result.getId());
        verify(orderRepository, times(1)).deleteById(TEST_ORDER_ID);
    }

    @Test
    void testGetOrdersByUserEmail_whenEmailExists_thenReturnsOrdersWithUser() throws JsonProcessingException {
        List<Order> orders = List.of(testOrder);
        when(orderRepository.findOrdersByUserId(testUserResponse.getUserId()))
                .thenReturn(orders);

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/users/find-by-email"))
                .withQueryParam("email", WireMock.matching("test(.+)test\\.by")) // Регулярное выражение
                .withHeader("Authorization", WireMock.equalTo("Bearer test-token"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testUserResponse))));

        List<OrderWithUserResponse> result = orderService.getOrdersByUserEmail(TEST_USER_EMAIL);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrderDto.getId(), result.get(0).getOrder().getId());
        assertEquals(TEST_USER_EMAIL, result.get(0).getUser().getEmail());

        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/users/find-by-email"))
                .withQueryParam("email", WireMock.matching("test(.+)test\\.by"))
                .withHeader("Authorization", WireMock.equalTo("Bearer test-token")));
    }

    @Test
    void testGetOrdersIdIn_whenIdsExists_thenReturnsOrdersWithUsers() {
        Set<Long> ids = Set.of(TEST_ORDER_ID);
        when(orderRepository.findAllByIdIn(ids)).thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.getOrdersIdIn(ids);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrderDto.getId(), result.get(0).getOrder().getId());

        verifyFeignCall("/api/users/1");
    }

    @Test
    void testFindByStatusIn_whenStatusesExists_thenReturnsOrdersWithUsers()  {
        Set<OrderStatus> statuses = Set.of(OrderStatus.NEW);
        when(orderRepository.findByStatusIn(statuses)).thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.findByStatusIn(statuses);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrderDto.getId(), result.get(0).getOrder().getId());

        verifyFeignCall("/api/users/1");
    }

    @Test
    void testGetAllOrders_thenReturnsAllOrdersWithUsers()  {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));

        List<OrderWithUserResponse> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrderDto.getId(), result.get(0).getOrder().getId());

        verifyFeignCall("/api/users/1");
    }

    @Test
    void testGetAllOrdersNativeWithPagination_thenReturnsPagedOrderDtos() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findAllOrdersNative(pageable)).thenReturn(page);

        Page<OrderDto> result = orderService.getAllOrdersNativeWithPagination(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testOrderDto.getId(), result.getContent().get(0).getId());
    }

    private void verifyFeignCall(String url) {
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(url))
                .withHeader("Authorization", WireMock.equalTo("Bearer test-token")));
    }
}
