package com.daewon.xeno_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductsStarRankListDTO {

    private Long productColorId;

    private String brandName;

    private Long price;

    private boolean isSale;

    private Long priceSale;

    private String category;

    private String categorySub;

    private byte[] productImage;
    
}
