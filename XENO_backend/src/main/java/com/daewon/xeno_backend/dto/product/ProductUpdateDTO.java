package com.daewon.xeno_backend.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateDTO {

    private Long productId;
    private String name;
    private String season;
    private String productNumber;
    private Long price;
    private boolean sale;
    private Long priceSale;
    private String category;
    private String categorySub;
}

