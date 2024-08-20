package com.daewon.xeno_backend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersCardListDTO {

    private Long orderId;
    private Long productColorId;
    private String orderDate;
    private String brandName;
    private String productName;
    private String size;
    private String color;
    private String status;
    private Long amount;
    private boolean isReview;
    private Long reviewId;
    private int quantity;
    private byte[] productImage;

}
