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
public class ProductRegisterColorDTO {

    private long productId;

    private String color; // 색상

    private List<ProductSizeDTO> size; // size 리스트
}
