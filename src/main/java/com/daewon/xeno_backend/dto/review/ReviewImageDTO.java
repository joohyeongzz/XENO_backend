package com.daewon.xeno_backend.dto.review;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImageDTO {
    private Long reviewImageId;

    private String uuid;

    private String fileName;
    
    private int ord;
}