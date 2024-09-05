package com.daewon.xeno_backend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDashBoardInfoDTO {
    private long refund;
    private long paymentComplete;

}
