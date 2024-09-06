package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.Orders;
import com.daewon.xeno_backend.domain.ProductsOption;
import com.daewon.xeno_backend.domain.auth.Brand;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.order.OrdersTopSellingProductsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {

    List<Orders> findByCustomer(Users user);

    @Query("SELECT o FROM Orders o WHERE o.customer = :user AND o.status NOT IN ('환불 요청', '환불 완료', '결제 취소') ORDER BY o.createAt DESC")
    Page<Orders> findPagingOrdersByCustomer(Pageable pageable, @Param("user") Users user);

    @Query("SELECT o FROM Orders o WHERE o.customer = :user AND o.status IN ('환불 요청', '환불 완료', '결제 취소') ORDER BY o.createAt DESC")
    Page<Orders> findPagingRefundedOrdersByCustomer(Pageable pageable, @Param("user") Users user);

    Optional<Orders> findByOrderId(Long orderId);

    @Query("SELECT o FROM Orders o WHERE o.productsOption.products.productId = :productId ")
    List<Orders> findByProductId(@Param("productId") long productId);

    @Query("SELECT o FROM Orders o WHERE o.orderId = :orderId and o.customer = :user")
    Orders findByOrderIdAndUserId(@Param("orderId") Long orderId, @Param("user") Users user);

    // 주문한 userId를 반환하도록 하는 메서드
    @Query("select o.customer.userId from Orders o where o.orderId = :orderId")
    Optional<Long> findAuthorUserIdByOrderId(@Param("orderId") Long orderId);

    Optional<Orders> findTopByCustomerEmailOrderByCreateAtDesc(String email);

    @Query("SELECT o FROM Orders o WHERE o.status = :status and o.productsOption = :option")
    List<Orders> findByStatusAndProductsOption(String status, ProductsOption option);

    @Query("SELECT o FROM Orders o WHERE o.brand = :users AND o.createAt BETWEEN :startDate AND :endDate AND o.status NOT IN ('환불 요청', '환불 완료','결제 취소') ORDER BY o.createAt DESC")
    List<Orders> findByBrandAndDateRange(Brand users,LocalDateTime startDate,LocalDateTime endDate);

    @Query("SELECT o FROM Orders o WHERE o.orderNumber = :orderNumber")
    Orders findByOrderNumber(long orderNumber);

    @Query(value = "SELECT * FROM orders o WHERE o.customer_id = :customerId ORDER BY o.create_at DESC LIMIT 1", nativeQuery = true)
    Orders findLatestOrderByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT o FROM Orders o WHERE o.brand = :users AND o.status = :status")
    List<Orders> findByStatusAndBrand(String status ,Brand users);

    @Query("SELECT COUNT(o) FROM Orders o WHERE o.status = :status and o.brand = :users")
    long countByStatus(String status ,Brand users);

    @Query("SELECT new com.daewon.xeno_backend.dto.order.OrdersTopSellingProductsDTO(" +
            "    p.name, " +   // Product의 이름
            "    SUM(o.quantity)) " +
            "FROM Orders o " +
            "JOIN o.productsOption po " +
            "JOIN po.products p " +   // Product 엔티티에 조인
            "WHERE o.brand = :brand " + // 특정 Brand로 필터링
            "GROUP BY p.productId " +
            "ORDER BY SUM(o.quantity) ASC")
    List<OrdersTopSellingProductsDTO> findTopSellingProducts(Brand brand, Pageable pageable);

}
