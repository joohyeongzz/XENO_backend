package com.daewon.xeno_backend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersDTO {

    private String orderPayId;
    private Long productOptionId;
    private String req;
    private int quantity;
    private Long amount;
    private int usedPoint;
    private String paymentKey;

}
