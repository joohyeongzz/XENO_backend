package com.daewon.xeno_backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 데이터베이스에서 각 Refresh Token을 고유하게 식별
    private Long id;
    // 실제 Refresh Token 값 저장
    private String token;
    // Refresh Token이 속한 사용자의 이메일
    private String email;
}
