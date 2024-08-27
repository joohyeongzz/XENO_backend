package com.daewon.xeno_backend.dto.order;

import java.util.List;

import lombok.Data;

@Data
public class OrdersCreateDTO {
    private Long userId;
    private int usedPoint;
    private Long amount;
    private List<OrdersDTO> ordersList;

}
