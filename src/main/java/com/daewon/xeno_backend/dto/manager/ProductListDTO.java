package com.daewon.xeno_backend.dto.manager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListDTO {
    private Long productId;
    private String brandName;
    private String name;
    private String category;
    private Long price;
    private Long priceSale;
    private String productNumber;
    private String color;
}
