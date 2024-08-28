package com.daewon.xeno_backend.dto.product;

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

    private String brandName;

    private String name;

    private String category;

    private String categorySub;

    private long price;

    private long priceSale;

    private boolean isSale;

    private String productNumber;

    private String season;

    private double starAvg;

    private long likeIndex;

    private long reviewIndex;

    private boolean isLike;

    private String color;

    private String url_1;
    private String url_2;
    private String url_3;
    private String url_4;
    private String url_5;
    private String url_6;

    private String detail_url;








}
