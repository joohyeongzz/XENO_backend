package com.daewon.xeno_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOtherColorImagesDTO {

    private long productColorId;

    private byte[] productColorImage;

}
