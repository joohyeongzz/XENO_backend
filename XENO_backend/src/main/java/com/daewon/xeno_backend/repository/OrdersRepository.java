package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.Orders;
import com.daewon.xeno_backend.domain.auth.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {

    List<Orders> findByUser(Users user);

    Page<Orders> findPagingOrdersByUser(Pageable pageable, Users user);

    Optional<Orders> findByOrderId(Long orderId);

    @Query("SELECT o FROM Orders o WHERE o.productsColorSize.productsColor.products.productId = :productId ")
    List<Orders> findByProductId(@Param("productId") long productId);

    @Query("SELECT o FROM Orders o WHERE o.orderId = :orderId and o.user = :user")
    Orders findByOrderIdAndUserId(@Param("orderId") Long orderId, @Param("user") Users user);

    // 주문한 userId를 반환하도록 하는 메서드
    @Query("select o.user.userId from Orders o where o.orderId = :orderId")
    Optional<Long> findAuthorUserIdByOrderId(@Param("orderId") Long orderId);

    Optional<Orders> findTopByUserEmailOrderByCreateAtDesc(String email);


}
