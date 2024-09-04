package com.daewon.xeno_backend.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewInfoDTO {

    private Long reviewId;
    private Long productId;
    private String userName;
    private String text;
    private boolean isReview;
    private double star;
    private int replyIndex;
    private String reviewImage;
    private String createAt;
    private String color;


}
