package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.review.ReviewCardDTO;
import com.daewon.xeno_backend.dto.review.ReviewCreateDTO;
import com.daewon.xeno_backend.dto.review.ReviewInfoDTO;
import com.daewon.xeno_backend.dto.review.ReviewUpdateDTO;
import com.daewon.xeno_backend.repository.*;

import com.daewon.xeno_backend.repository.auth.UserRepository;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Log4j2
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {


    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductsImageRepository productsImageRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final ReplyRepository replyRepository;
    private final ProductsStarRepository productsStarRepository;


    private ReviewInfoDTO convertToDTO(Review review, Users currentUser) {
        ReviewInfoDTO dto = new ReviewInfoDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        if (currentUser != null && review.getUsers().equals(currentUser)) {
            dto.setReview(true);
        } else {
            dto.setReview(false);
        }
        dto.setReviewId(review.getReviewId());
        dto.setProductId(review.getOrder().getProductsOption().getProducts().getProductId());
        dto.setUserName(review.getUsers().getName());
//        dto.setProductName(review.getOrder().getProductsOption().getProducts().getProducts().getName());
        dto.setColor(review.getOrder().getProductsOption().getProducts().getColor());
//        dto.setSize(review.getOrder().getProductsOption().getSize().name());
        dto.setText(review.getText());
        dto.setStar(review.getStar());
        int replyIndex = replyRepository.countByReviewId(review.getReviewId());
        dto.setReplyIndex(replyIndex);
        dto.setCreateAt(review.getCreateAt().format(formatter));



//        ProductsImage productsImage = productsImageRepository.findByProductId(review.getOrder().getProductsOption().getProducts().getProductId());
//        if (productsImage != null) {
//            try {
//                byte[] productImageData = getImage(productsImage.getUuid(), productsImage.getFileName());
//                dto.setProductImage(productImageData);
//            } catch (java.io.IOException e) {
//                throw new RuntimeException(e);
//            }
//        } else {
//            dto.setReviewImage(null);
//        }

        return dto;
    }

    @Override
    public String createReview(ReviewCreateDTO reviewCreateDTO, MultipartFile image, UserDetails userDetails) {

        String userEmail = userDetails.getUsername();

        Users users = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));

        Review review = new Review();
        review.setText(reviewCreateDTO.getText());
        review.setStar(reviewCreateDTO.getStar());
        review.setUsers(users.getUserId());
        review.setOrders(reviewCreateDTO.getOrderId());
        reviewRepository.save(review);

        Orders orders = ordersRepository.findByOrderId(reviewCreateDTO.getOrderId()).orElse(null);

        if (orders != null) {
            ProductsStar productsStar = productsStarRepository
                    .findByProductId(orders.getProductsOption().getProducts().getProductId())
                    .orElse(null);
            if (productsStar == null) {
                productsStar = ProductsStar.builder()
                        .products(orders.getProductsOption().getProducts())
                        .starAvg(reviewCreateDTO.getStar())
                        .starTotal(reviewCreateDTO.getStar())
                        .build();
            } else {
                productsStar.setStarTotal(productsStar.getStarTotal() + reviewCreateDTO.getStar());
                double starAvg = Math.round((productsStar.getStarTotal() / reviewRepository.countByProductId(orders.getProductsOption().getProducts().getProductId())) * 10.0) / 10.0;
                productsStar.setStarAvg(starAvg);
            }
            productsStarRepository.save(productsStar);
        } else {
            return "잘못된 주문 내역입니다.";
        }
            return "성공";
    }

    @Override
    public String updateReview(Long reviewId, ReviewUpdateDTO reviewDTO, MultipartFile image) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        review.setText(reviewDTO.getText());

        Orders orders = ordersRepository.findByOrderId(review.getOrder().getOrderId()).orElse(null);

        ProductsStar productsStar = productsStarRepository
                .findByProductId(orders.getProductsOption().getProducts().getProductId())
                .orElse(null);

        productsStar.setStarTotal(productsStar.getStarTotal() - review.getStar() + reviewDTO.getStar());
        double starAvg = Math.round((productsStar.getStarTotal() / reviewRepository.countByProductId(orders.getProductsOption().getProducts().getProductId())) * 10.0) / 10.0;
        productsStar.setStarAvg(starAvg);
        review.setStar(reviewDTO.getStar());
        reviewRepository.save(review);


        return "성공";
}

    @Override
    public PageInfinityResponseDTO<ReviewCardDTO> readAllReviewImageList(PageRequestDTO pageRequestDTO) {

        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPageIndex() <= 0 ? 0 : pageRequestDTO.getPageIndex() - 1,
                pageRequestDTO.getSize(),
                Sort.by("reviewImageId").ascending());
        Page<ReviewImage> result = reviewImageRepository.findAll(pageable);
        List<ReviewCardDTO> dtoList = new ArrayList<>();

      return PageInfinityResponseDTO.<ReviewCardDTO>withAll()
              .pageRequestDTO(pageRequestDTO)
              .dtoList(dtoList)
              .totalIndex((int) result.getTotalElements())
              .build();
    }

    // 리뷰 조회
    @Override
    public ReviewInfoDTO readReviewInfo(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Users users = userRepository.findByEmail(currentUserName).orElse(null);

        return convertToDTO(review, users);
    }

    // 리뷰 목록 조회
    @Override
    public PageResponseDTO<ReviewInfoDTO> readReviewList(Long productId, PageRequestDTO pageRequestDTO) {
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPageIndex() <= 0 ? 0 : pageRequestDTO.getPageIndex() - 1,
                pageRequestDTO.getSize(),
                Sort.by("reviewId").ascending());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Users users = userRepository.findByEmail(currentUserName).orElse(null);
        Page<Review> result = reviewRepository.findByProductId(productId, pageable);
        List<ReviewInfoDTO> dtoList = result.getContent().stream()
                .map(review -> convertToDTO(review, users))
                .collect(Collectors.toList());

        return PageResponseDTO.<ReviewInfoDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .totalIndex((int) result.getTotalElements())
                .build();
    }


    // 리뷰 삭제
    @Override
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id : " + reviewId));

        long productId = review.getOrder().getProductsOption().getProducts().getProductId();
        ReviewImage reviewImage = reviewImageRepository.findByReview(review);



        ProductsStar productsStar = productsStarRepository.findByProductId(productId).orElse(null);
        productsStar.setStarTotal(productsStar.getStarTotal() - review.getStar());
        reviewRepository.deleteById(reviewId);

        double starAvg = Math.round((productsStar.getStarTotal() / reviewRepository.countByProductId(productId) * 10.0) / 10.0);
        log.info(starAvg);
        productsStar.setStarAvg(starAvg);
        productsStarRepository.save(productsStar);
        }

    }
