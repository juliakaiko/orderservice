package com.mymicroservice.orderservice.repository;

import com.mymicroservice.orderservice.model.Order;
import com.mymicroservice.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds all orders with specified IDs using "named method".
     *
     * @param ids set of order IDs to search for
     * @return a list of orders matching the provided IDs (may be empty)
     * @throws IllegalArgumentException if ids set is null
     */
    List<Order> findAllByIdIn(Set<Long> ids);


    /**
     * Retrieves a list of orders that have a status matching any of the specified statuses.
     * and using "named method query"
     *
     * @param statuses a set of {@link OrderStatus} values to filter the orders by.
     * @return a list of {@link Order} entities whose status matches any in the provided set.
     */
    List<Order> findByStatusIn(Set<OrderStatus> statuses);

    /**
     * Finds all orders associated with the given user ID.
     *
     * @param userId the ID of the user to search orders for
     * @return a list of orders belonging to the specified user
     *         (empty list if no orders found)
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId")
    List<Order> findOrdersByUserId(@Param("userId") Long userId);

    /**
     * Retrieves all orders with pagination support using native SQL.
     * <p>
     * Results are ordered by order ID in ascending order.
     *
     * @param pageable pagination configuration (page number, size, etc.)
     * @return a {@link Page} of orders with pagination information
     * @throws IllegalArgumentException if pageable is null
     */
    @Query(value = "select * from orders order by orders.id asc", nativeQuery = true)
    Page<Order> findAllOrdersNative(Pageable pageable);

}
