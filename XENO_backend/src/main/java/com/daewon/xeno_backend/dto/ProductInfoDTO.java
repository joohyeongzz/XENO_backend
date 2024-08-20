package com.daewon.xeno_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInfoDTO {

    private long productId;

    private long productColorId;

    private String brandName;

    private String name;

    private String category;

    private String categorySub;

    private long price;

    private long priceSale;

    private boolean isSale;

    private long productsNumber;

    private String season;

    private long starAvg;

    private long likeIndex;

    private long reviewIndex;

    private boolean booleanColor;

    private boolean isLike;

    private String color;

    private List<String> colorType;

    private List<byte[]> productImages;

}
