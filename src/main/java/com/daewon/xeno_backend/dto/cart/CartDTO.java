package com.daewon.xeno_backend.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartDTO {

    private Long cartId;
    private Long productOptionId;
    private Long quantity;
    private Long amount;
    private String brandName;
    private String productName;
    private String color;
    private String size;
//    private String imageUuid;
//    private String imageFileName;
    private byte[] productImage;
    private Long price;
    private boolean isSale;
}
