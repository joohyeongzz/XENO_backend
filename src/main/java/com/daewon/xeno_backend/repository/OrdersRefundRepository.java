package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.OrdersRefund;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrdersRefundRepository  extends JpaRepository<OrdersRefund, Long> {

    @Query("SELECT o FROM OrdersRefund o WHERE o.order.orderId = :orderId ")
    OrdersRefund findByOrderId(Long orderId);



}
