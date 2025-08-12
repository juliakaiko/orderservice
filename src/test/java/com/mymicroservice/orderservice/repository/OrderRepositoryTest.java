package com.mymicroservice.orderservice.repository;

import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.util.OrderGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import com.mymicroservice.orderservice.configuration.TestContainersConfig;
import com.mymicroservice.orderservice.model.OrderStatus;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@Slf4j
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Disabling DataSource Replacement
@Import(TestContainersConfig.class)
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private static Order expectedOrder;

    @BeforeAll
    static void setUp(){
        expectedOrder = OrderGenerator.generateOrder();
    }

    @BeforeEach
    void init() {
        orderRepository.deleteAll();
        expectedOrder = orderRepository.save(expectedOrder);
    }

    @Test
    void findAllOrdersByIdIn_shouldReturnOrdersWithGivenIds() {
        log.info("Test findAllOrdersByIdIn - should return orders with specified IDs");
        orderRepository.saveAll(List.of(expectedOrder));

        List<Order> result = orderRepository.findAllByIdIn(Set.of(expectedOrder.getId()));

        assertThat(result).hasSize(1);
        assertEquals(expectedOrder, result.get(0));
    }

    @Test
    void findAllOrdersByIdIn_shouldReturnEmptyListWhenNoMatches() {
        log.info("Test findAllOrdersByIdIn - should return empty list for non-existent IDs");
        List<Order> result = orderRepository.findAllByIdIn(Set.of(999L));
        assertThat(result).isEmpty();
    }

    @Test
    void findByStatusIn_shouldReturnOrdersWithGivenStatuses() {
        log.info("Test findByStatusIn - should return orders with specified statuses");
        orderRepository.saveAll(List.of(expectedOrder));

        List<Order> result = orderRepository.findByStatusIn(Set.of(expectedOrder.getStatus()));

        assertThat(result).hasSize(1);
        assertEquals(expectedOrder, result.get(0));
    }

    @Test
    void findByStatusIn_shouldReturnEmptyListWhenNoStatusMatches() {
        log.info("Test findByStatusIn - should return empty list for non-matching statuses");
        List<Order> result = orderRepository.findByStatusIn(Set.of(OrderStatus.CANCELLED));
        assertThat(result).isEmpty();
    }

    @Test
    void findOrdersByUserId_shouldReturnOrdersForGivenUser() {
        log.info("Test findOrdersByUserId - should return orders for specified user");
        List<Order> result = orderRepository.findOrdersByUserId(expectedOrder.getUserId());

        assertThat(result).hasSize(1);
        assertThat(result).extracting(Order::getUserId)
                .containsOnly(expectedOrder.getUserId());
    }

    @Test
    void findOrdersByUserId_shouldReturnEmptyListForNonExistentUser() {
        log.info("Test findOrdersByUserId - should return empty list for non-existent user");
        List<Order> result = orderRepository.findOrdersByUserId(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void findAllOrdersNative_shouldReturnPagedResults() {
        log.info("Test findAllOrdersNative - should return paged results");
        Pageable pageable = PageRequest.of(0, 2);

        Page<Order> page = orderRepository.findAllOrdersNative(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getNumber()).isEqualTo(0);
    }

    @Test
    public void findAllOrdersNative_shouldReturnEmptyPageWhenNoOrders() {
        log.info("Test findAllOrdersNative - should return empty page when no orders exist");
        orderRepository.deleteAll();
        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> page = orderRepository.findAllOrdersNative(pageable);

        assertThat(page.getContent()).isEmpty();
    }
}
