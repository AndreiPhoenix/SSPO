package com.ordermanagement.repository;

import com.ordermanagement.entity.Order;
import com.ordermanagement.entity.Order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.customer.email = :email")
    List<Order> findByCustomerEmail(@Param("email") String email);

    @Query("SELECT o FROM Order o JOIN o.items i WHERE i.product.id = :productId")
    List<Order> findByProductId(@Param("productId") Long productId);

    @Query("SELECT o FROM Order o WHERE o.totalAmount > :minAmount ORDER BY o.orderDate DESC")
    List<Order> findOrdersWithAmountGreaterThan(@Param("minAmount") BigDecimal minAmount);
}