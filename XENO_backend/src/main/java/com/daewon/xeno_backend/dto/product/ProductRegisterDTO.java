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
public class ProductRegisterDTO {



    private String brandName;

    private String category;

    private String categorySub;

    private String name;
    
    private Long price;

    private boolean sale;

    private Long priceSale;

    private String productNumber;

    private String season;

    private String colors; // 색상

    private List<ProductSizeDTO> size; // size 리스트

    private String url_1;
    private String url_2;
    private String url_3;
    private String url_4;
    private String url_5;
    private String url_6;

    private String detail_url_1;







//    @JsonProperty("isSale")
//    public boolean isSale() {
//        return isSale;
//    }
//
//    @JsonProperty("isSale")
//    public void setSale(boolean sale) {
//        isSale = sale;
//    }


}
