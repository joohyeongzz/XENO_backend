package com.daewon.xeno_backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DeliveryTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryTrackId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
    private Orders order;

    @Column(length = 64, nullable = false)
    private String carrierId; // 택배사 ID

    @Column(length = 64, nullable = false)
    private String trackingNumber; // 운송장 번호

    @Column
    private String lastEventTime; // 마지막 이벤트 시간

}
