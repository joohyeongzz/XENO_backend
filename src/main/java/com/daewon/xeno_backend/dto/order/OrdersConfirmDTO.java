package com.daewon.xeno_backend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersConfirmDTO {

    private Long orderId;
    private String orderPayId;
    private String orderNumber;
    private String name;
    private String address;
    private double amount;
    private int quantity;
}
