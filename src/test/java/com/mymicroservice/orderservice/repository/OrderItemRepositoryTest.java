package com.mymicroservice.orderservice.repository;

import com.mymicroservice.orderservice.configuration.TestContainersConfig;
import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderItem;
import com.mymicroservice.orderservice.util.OrderGenerator;
import com.mymicroservice.orderservice.util.OrderItemGenerator;
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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@Slf4j
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Disabling DataSource Replacement
@Import(TestContainersConfig.class)
public class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    private static OrderItem expectedOrderItem;

    @BeforeAll
    static void setUp(){
        expectedOrderItem = OrderItemGenerator.generateOrderItem();
    }

    @BeforeEach
    void init() {
        orderItemRepository.deleteAll();
        expectedOrderItem = orderItemRepository.save(expectedOrderItem);
    }

    @Test
    void findAllOrderItemsByIdIn_shouldReturnOrderItemsWithGivenIds() {
        log.info("Test findAllOrderItemsByIdIn - should return OrderItems with specified IDs");
        orderItemRepository.saveAll(List.of(expectedOrderItem));

        List<OrderItem> result = orderItemRepository.findAllByIdIn(Set.of(expectedOrderItem.getId()));

        assertThat(result).hasSize(1);
        assertEquals(expectedOrderItem, result.get(0));
    }

    @Test
    void findAllOrderItemsByIdIn_shouldReturnEmptyListWhenNoMatches() {
        log.info("Test findAllOrderItemsByIdIn - should return empty list for non-existent IDs");
        List<OrderItem> result = orderItemRepository.findAllByIdIn(Set.of(999L));
        assertThat(result).isEmpty();
    }

    @Test
    void findAllOrderItemsNative_shouldReturnPagedResults() {
        log.info("Test findAllOrderItemsNative - should return paged results");
        Pageable pageable = PageRequest.of(0, 2);

        Page<OrderItem> page = orderItemRepository.findAllOrderItemsNative(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getNumber()).isEqualTo(0);
    }

    @Test
    public void findAllOrderItemsNative_shouldReturnEmptyPageWhenNoOrders() {
        log.info("Test findAllOrdersNative - should return empty page when no orders exist");
        orderItemRepository.deleteAll();
        Pageable pageable = PageRequest.of(0, 10);

        Page<OrderItem> page = orderItemRepository.findAllOrderItemsNative(pageable);

        assertThat(page.getContent()).isEmpty();
    }
}
