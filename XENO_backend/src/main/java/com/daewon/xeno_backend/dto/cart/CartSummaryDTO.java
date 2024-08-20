package com.daewon.xeno_backend.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartSummaryDTO {   // 장바구니의 요약 정보를 담는 DTO

    private int totalProductIndex;
    private Long totalPrice;
}
