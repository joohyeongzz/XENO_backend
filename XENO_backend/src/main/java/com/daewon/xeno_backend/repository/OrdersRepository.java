package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.Orders;
import com.daewon.xeno_backend.domain.ProductsOption;
import com.daewon.xeno_backend.domain.auth.Users;
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

    Page<Orders> findPagingOrdersByCustomer(Pageable pageable, Users user);

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

    @Query("SELECT o FROM Orders o " +
            "WHERE o.createAt BETWEEN :startDate AND :endDate " +
            "AND o.status IN ('결제 완료', '출고 완료', '배송 중', '배송 완료', '주문 확정', '배송 준비 중') AND o.seller = :users")
    List<Orders> findOrdersByYear(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,Users users);


    @Query("SELECT o FROM Orders o WHERE o.orderNumber = :orderNumber")
    Orders findByOrderNumber(long orderNumber);






}
