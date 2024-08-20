package com.daewon.xeno_backend.dto.review;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCardDTO {
    private Long reviewId;
    private Long productColorId;
    private byte[] reviewImage;
}