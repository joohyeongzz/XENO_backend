package com.daewon.xeno_backend.controller;

import org.springframework.web.bind.annotation.*;

import com.daewon.xeno_backend.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("products/{productId}/reviews")
public class ReviewImageController {

    private final ReviewService reviewService;

//    @Operation(summary = "전체 이미지 총 갯수")
//    @GetMapping("/images/count")
//    public ResponseEntity<Long> getReviewImageCount(@PathVariable Long productId) {
//        Long count = reviewService.countReviewImagesByProductId(productId);
//        return ResponseEntity.ok(count);
//    }

}
