package com.daewon.xeno_backend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersStatusUpdateDTO {
    private Long orderId;
    private String status;

}
