package com.daewon.xeno_backend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersCardListDTO {

    private Long orderId;
    private Long productId;
    private Long productOptionId;
    private String orderDate;
    private String brandName;
    private String productName;
    private String customerName;
    private String address;
    private String carrierId;
    private String trackingNumber;
    private String size;
    private String color;

    private String status;
    private Long amount;
    private boolean isReview;
    private Long reviewId;
    private int quantity;
    private String productImage;

}
