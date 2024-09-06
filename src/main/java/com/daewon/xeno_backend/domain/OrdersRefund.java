package com.daewon.xeno_backend.domain;


import com.daewon.xeno_backend.domain.auth.Brand;
import com.daewon.xeno_backend.domain.auth.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OrdersRefund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long OrdersRefundId;

    @ManyToOne()  // fetch = FetchType.LAZY
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Orders order;

    private String reason;


}
