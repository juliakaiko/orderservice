package com.mymicroservice.orderservice.repository;

import com.mymicroservice.orderservice.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByIdIn(Set<Long> ids);

    @Query(value = "select * from order_items order by order_items.id asc", nativeQuery = true)
    Page<OrderItem> findAllOrderItemsNative(Pageable pageable);

}
