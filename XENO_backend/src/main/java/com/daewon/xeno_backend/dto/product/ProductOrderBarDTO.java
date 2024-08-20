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
public class ProductOrderBarDTO {


    private boolean isLike;
    private long likeIndex;
    private List<ProductStockDTO> orderInfo;
    private long price;

}
