package com.daewon.xeno_backend.dto.order;

import com.daewon.xeno_backend.domain.Orders;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrdersResponseDTO {
    private Long orderId;
    private Long orderNumber;
    private String status;
    private String req;
    private String orderPayId;

    public OrdersResponseDTO(Orders orders) {
        this.orderId = orders.getOrderId();
        this.orderNumber = orders.getOrderNumber();
        this.status = orders.getStatus();
        this.req = orders.getReq();
        this.orderPayId = orders.getOrderPayId();
    }
}
