package com.daewon.xeno_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailImagesDTO {

    private Page<byte[]> productImages;
    private long imagesCount;





}
