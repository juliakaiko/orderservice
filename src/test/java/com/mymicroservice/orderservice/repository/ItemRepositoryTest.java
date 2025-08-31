package com.mymicroservice.orderservice.repository;

import com.mymicroservice.orderservice.configuration.AbstractContainerTest;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.util.ItemGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ItemRepositoryTest extends AbstractContainerTest{

    @Autowired
    private ItemRepository itemRepository;

    private static Item expectedItem;

    @BeforeAll
    static void setUp(){
        expectedItem = ItemGenerator.generateItem();
    }

    @BeforeEach
    void init() {
        itemRepository.deleteAll();
        expectedItem = itemRepository.save(expectedItem);
    }

    @Test
    void findAllItemsByIdIn_shouldReturnItemsWithGivenIds() {
        log.info("Test findAllItemsByIdIn - should return Items with specified IDs");
        itemRepository.saveAll(List.of(expectedItem));

        List<Item> result = itemRepository.findAllByIdIn(Set.of(expectedItem.getId()));

        assertThat(result).hasSize(1);
        assertEquals(expectedItem, result.get(0));
    }

    @Test
    void findAllItemsByIdIn_shouldReturnEmptyListWhenNoMatches() {
        log.info("Test findAllItemsByIdIn - should return empty list for non-existent IDs");
        List<Item> result = itemRepository.findAllByIdIn(Set.of(999L));
        assertThat(result).isEmpty();
    }

    @Test
    void findAllItemsNative_shouldReturnPagedResults() {
        log.info("Test findAllItemsNative - should return paged results");
        Pageable pageable = PageRequest.of(0, 2);

        Page<Item> page = itemRepository.findAllItemsNative(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getNumber()).isEqualTo(0);
    }

    @Test
    public void findAllItemsNative_shouldReturnEmptyPageWhenNoOrders() {
        log.info("Test findAllOrdersNative - should return empty page when no orders exist");
        itemRepository.deleteAll();
        Pageable pageable = PageRequest.of(0, 10);

        Page<Item> page = itemRepository.findAllItemsNative(pageable);

        assertThat(page.getContent()).isEmpty();
    }
}
