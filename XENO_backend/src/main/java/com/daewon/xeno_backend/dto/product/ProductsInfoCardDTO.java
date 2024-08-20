package com.daewon.xeno_backend.dto.product;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductsInfoCardDTO {

    private long productColorId;

    private String brandName;

    private String name;

    private String category;

    private String categorySub;

    private long price;

    private long priceSale;

    private boolean isSale;

    private boolean isLike;

    private long likeIndex;

    private double starAvg;

    private byte[] productImage;

}
