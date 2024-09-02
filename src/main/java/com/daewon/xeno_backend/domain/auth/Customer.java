package com.daewon.xeno_backend.domain.auth;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    // 사용자 ID를 직접 저장
    @Column(name = "userId")
    private Long userId;

    // 적립금
    private int point;

    // 유저 등급
    @Enumerated(EnumType.STRING)
    private Level level;
}
