package com.mymicroservice.orderservice.repository;

import com.mymicroservice.orderservice.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByIdIn(Set<Long> ids);

    @Query(value = "select * from items order by items.id asc", nativeQuery = true)
    Page<Item> findAllItemsNative(Pageable pageable);
}
