package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.review.ReviewCardDTO;

import com.daewon.xeno_backend.dto.review.ReviewCreateDTO;
import com.daewon.xeno_backend.dto.review.ReviewInfoDTO;
import com.daewon.xeno_backend.dto.review.ReviewUpdateDTO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewService {

    String createReview(ReviewCreateDTO reviewCreateDTO, MultipartFile image, UserDetails userDetails);

    ReviewInfoDTO readReviewInfo(Long reviewId);

    String updateReview(Long reviewId, ReviewUpdateDTO reviewDTO, MultipartFile image) throws Exception;

    void deleteReview(Long reviewId);


    PageInfinityResponseDTO<ReviewCardDTO> readAllReviewImageList(PageRequestDTO pageRequestDTO);

    PageResponseDTO<ReviewInfoDTO> readReviewList(Long productId,PageRequestDTO pageRequestDTO);


}