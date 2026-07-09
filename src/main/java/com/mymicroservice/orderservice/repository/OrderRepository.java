package com.mymicroservice.orderservice.repository;

import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByIdIn(Set<UUID> ids);

    List<Order> findByStatusIn(Set<OrderStatus> statuses);

    List<Order> findByStatusInAndUserId(Set<OrderStatus> statuses, Long userId);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId")
    List<Order> findOrdersByUserId(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.id IN :ids AND o.userId = :userId")
    List<Order> findAllByIdInAndUserId(@Param("ids") Set<UUID> ids, @Param("userId") Long userId);

    @Query(value = "select * from orders order by orders.id asc", nativeQuery = true)
    Page<Order> findAllOrdersNative(Pageable pageable);

    @Query(value = "select * from orders where user_id = :userId order by orders.id asc", nativeQuery = true)
    Page<Order> findAllOrdersNativeByUserId(@Param("userId") Long userId, Pageable pageable);
}
