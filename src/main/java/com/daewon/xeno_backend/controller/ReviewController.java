package com.daewon.xeno_backend.controller;


import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.product.ProductHeaderDTO;
import com.daewon.xeno_backend.dto.review.ReviewCardDTO;
import com.daewon.xeno_backend.dto.review.ReviewInfoDTO;
import com.daewon.xeno_backend.dto.review.ReviewUpdateDTO;

import com.daewon.xeno_backend.service.OrdersService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.daewon.xeno_backend.dto.review.ReviewCreateDTO;
import com.daewon.xeno_backend.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;


@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final OrdersService ordersService;

    @PostMapping(value = "/create",consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createReview(
            // @RequestPart 어노테이션은 요청의 일부인 특정 파트를 가져오는 데 사용됨.
            // 클라이언트로부터 전달된 'reviewDTO'라는 이름의 JSON 데이터를 문자열 형식으로 받음
            @RequestPart("reviewDTO") String reviewCreateDTOStr,
            // 클라이언트로부터 전달된 파일 리스트를 받음.
            // required = false 로 설정하여 파일이 없어도 요청이 처리됨
            @RequestPart(name = "image", required = false) MultipartFile image ,@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Review DTO String: " + reviewCreateDTOStr);
        log.info("Files: " + image);

        ReviewCreateDTO reviewDTO;
        try {
            // JSON 문자열을 ReviewDTO 객체로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            reviewDTO = objectMapper.readValue(reviewCreateDTOStr, ReviewCreateDTO.class);
            log.info(reviewDTO);
        } catch (IOException e) {
            // JSON 변환 중 오류가 발생하면 로그를 남기고 예외 발생
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON format", e);
        }

        try {
            // 리뷰 생성 메서드 호출
            reviewService.createReview(reviewDTO, image != null && !image.isEmpty() ? image : null,userDetails);


            return ResponseEntity.ok("\"성공\"");
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad Request", e);
        }
    }

    @Operation(summary = "리뷰 조회")
    @GetMapping("/info")
    public ResponseEntity<ReviewInfoDTO> readReview(@RequestParam Long reviewId) {
        try {
            ReviewInfoDTO reviewInfoDTO = reviewService.readReviewInfo(reviewId);
            return ResponseEntity.ok(reviewInfoDTO);
        } catch (RuntimeException e) {
            log.error("Error fetching review details for reviewId {}: ", reviewId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }


    // 리뷰 작성한 유저만 수정 가능
    @PreAuthorize("@reviewAndReplySecurityUtils.isReviewOwner(#reviewId)")
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "리뷰 수정")
    public ResponseEntity<?> updateReview(
                @RequestParam Long reviewId,
                @RequestPart(name = "reviewDTO") String reviewDTOStr,
                @RequestPart(name = "image", required = false) MultipartFile image) {
            ReviewUpdateDTO reviewDTO;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                reviewDTO = objectMapper.readValue(reviewDTOStr, ReviewUpdateDTO.class);
            } catch (IOException e) {
                log.error("Error parsing reviewDTO", e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON format", e);
            }

            try {
                String updatedReview = reviewService.updateReview(reviewId, reviewDTO, image);
                return ResponseEntity.ok(updatedReview);
            } catch (Exception e) {
                log.error("Error updating review", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating review", e);
            }
    }

    // 리뷰 삭제
    // 리뷰 작성한 유저마 삭제 가능
    @PreAuthorize("@reviewAndReplySecurityUtils.isReviewOwner(#reviewId)")
    @DeleteMapping("/delete")
    @Operation(summary = "리뷰 삭제")
    public ResponseEntity<?> deleteReview(@RequestParam Long reviewId) {
        try {
            reviewService.deleteReview(reviewId);
            return ResponseEntity.ok().body("리뷰 삭제 완료");
        }  catch (RuntimeException e) {
            log.error("리뷰 삭제 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
}

    @Operation(summary = "리뷰 리스트")
    @GetMapping("/read/list")
    public ResponseEntity<PageResponseDTO<ReviewInfoDTO>> getReviewListByproductId(@RequestParam Long productId, PageRequestDTO pageRequestDTO) {
        PageResponseDTO<ReviewInfoDTO> reviews = reviewService.readReviewList(productId,pageRequestDTO);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/page/card")
    @Operation(summary = "제품의 모든 리뷰 이미지 가져오기")
    public ResponseEntity<PageInfinityResponseDTO<ReviewCardDTO>> getAllReviewList(PageRequestDTO pageRequestDTO) {
        PageInfinityResponseDTO<ReviewCardDTO> reviewList = reviewService.readAllReviewImageList(pageRequestDTO);
        return ResponseEntity.ok(reviewList);
    }


    @GetMapping("/header")
    public ResponseEntity<?> getProductHeader(@RequestParam Long orderId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userEmail = userDetails.getUsername();

            log.info("orderUserEmail : " + userEmail);
            ProductHeaderDTO header = ordersService.getProductHeader(orderId,userEmail);

            return ResponseEntity.ok(header);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("해당하는 상품 또는 재고가 없습니다.");
        }
    }
}

/*
    createReview 메서드 값 넘기는법

    {   "orderId": 해당하는 orderId값 입력,   "text": "Great product!",   "star": 5 }
 */

