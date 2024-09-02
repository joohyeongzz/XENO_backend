package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.DeliveryTrack;
import com.daewon.xeno_backend.domain.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeliveryTrackRepository extends JpaRepository<DeliveryTrack, Long> {

    @Query("SELECT d FROM DeliveryTrack d WHERE d.order.status IN ('출고 완료', '배송 중', '배송 완료','배송 준비 완료')")
    List<DeliveryTrack> findOrdersWithStatusIn();
}
