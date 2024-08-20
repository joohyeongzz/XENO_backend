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
public class ProductColorUpdateGetInfoDTO {

    private String color;
    private String fileName;
    private List<ProductSizeDTO> size;
    private List<ProductImageInfoDTO> images;
    private ProductImageInfoDTO detailImage;
}
