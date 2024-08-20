package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.review.ReviewCardDTO;
import com.daewon.xeno_backend.dto.review.ReviewCreateDTO;
import com.daewon.xeno_backend.dto.review.ReviewInfoDTO;
import com.daewon.xeno_backend.dto.review.ReviewUpdateDTO;
import com.daewon.xeno_backend.repository.*;

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

    @Value("${org.daewon.upload.path}")
    private String uploadPath;

    public byte[] getImage(String uuid, String fileName) throws IOException, java.io.IOException {
        String filePath = uploadPath + uuid + "_" + fileName;
        // 파일을 바이트 배열로 읽기
        Path path = Paths.get(filePath);
        byte[] image = Files.readAllBytes(path);
        return image;
    }

    private ReviewInfoDTO convertToDTO(Review review, Users currentUser) {
        ReviewInfoDTO dto = new ReviewInfoDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        if (currentUser != null && review.getUsers().equals(currentUser)) {
            dto.setReview(true);
        } else {
            dto.setReview(false);
        }
        dto.setReviewId(review.getReviewId());
        dto.setProductColorId(review.getOrder().getProductsColorSize().getProductsColor().getProductColorId());
        dto.setUserName(review.getUsers().getName());
//        dto.setProductName(review.getOrder().getProductsColorSize().getProductsColor().getProducts().getName());
        dto.setColor(review.getOrder().getProductsColorSize().getProductsColor().getColor());
//        dto.setSize(review.getOrder().getProductsColorSize().getSize().name());
        dto.setText(review.getText());
        dto.setStar(review.getStar());
        int replyIndex = replyRepository.countByReviewId(review.getReviewId());
        dto.setReplyIndex(replyIndex);
        dto.setCreateAt(review.getCreateAt().format(formatter));

        ReviewImage reviewImage = reviewImageRepository.findByReview(review);
        if (reviewImage != null) {
            try {
                byte[] reviewImageData = getImage(reviewImage.getUuid(), reviewImage.getFileName());
                dto.setReviewImage(reviewImageData);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            dto.setReviewImage(null);
        }

//        ProductsImage productsImage = productsImageRepository.findFirstByProductColorId(review.getOrder().getProductsColorSize().getProductsColor().getProductColorId());
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
                    .findByProductColorId(orders.getProductsColorSize().getProductsColor().getProductColorId())
                    .orElse(null);
            if (productsStar == null) {
                productsStar = ProductsStar.builder()
                        .productsColor(orders.getProductsColorSize().getProductsColor())
                        .starAvg(reviewCreateDTO.getStar())
                        .starTotal(reviewCreateDTO.getStar())
                        .build();
            } else {
                productsStar.setStarTotal(productsStar.getStarTotal() + reviewCreateDTO.getStar());
                double starAvg = Math.round((productsStar.getStarTotal() / reviewRepository.countByProductColorId(orders.getProductsColorSize().getProductsColor().getProductColorId())) * 10.0) / 10.0;
                productsStar.setStarAvg(starAvg);
            }
            productsStarRepository.save(productsStar);
        } else {
            return "잘못된 주문 내역입니다.";
        }
        if (image != null && !image.isEmpty()) {
            // 원본 파일명 가져오기
            String originalName = image.getOriginalFilename();
            // UUID 생성 (파일명 중복 방지)
            String uuid = UUID.randomUUID().toString();
            // 파일을 저장할 경로 생성 (컨트롤러에서 @Value로 지정해준 디렉토리에 UUID_원본파일명 형식으로 저장)
            Path savePath = Paths.get(uploadPath, uuid + "_" + originalName);

            try {
                // 파일을 지정된 경로에 저장
                image.transferTo(savePath.toFile());

                // ReviewImage 엔티티 생성 및 저장
                ReviewImage reviewImage = ReviewImage.builder()
                        .uuid(uuid)
                        .fileName(originalName)
                        .review(review)
                        .build();
                reviewImageRepository.save(reviewImage);
            } catch (IOException e) {
                // 파일 저장 또는 썸네일 생성 중 오류가 발생할 경우
                log.error("파일 저장하는 도중 오류가 발생했습니다: ", e);
                throw new RuntimeException("File processing error", e);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }

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
                .findByProductColorId(orders.getProductsColorSize().getProductsColor().getProductColorId())
                .orElse(null);

        productsStar.setStarTotal(productsStar.getStarTotal() - review.getStar() + reviewDTO.getStar());
        double starAvg = Math.round((productsStar.getStarTotal() / reviewRepository.countByProductColorId(orders.getProductsColorSize().getProductsColor().getProductColorId())) * 10.0) / 10.0;
        productsStar.setStarAvg(starAvg);
        review.setStar(reviewDTO.getStar());
        reviewRepository.save(review);

        if (image != null && !image.isEmpty()) {
            // 원본 파일명 가져오기
            String originalName = image.getOriginalFilename();
            // UUID 생성 (파일명 중복 방지)
            String uuid = UUID.randomUUID().toString();
            // 파일을 저장할 경로 생성 (컨트롤러에서 @Value로 지정해준 디렉토리에 UUID_원본파일명 형식으로 저장)
            Path savePath = Paths.get(uploadPath, uuid + "_" + originalName);

            log.info(uuid);
            log.info(originalName);
            // 파일을 지정된 경로에 저장
            try {
                // 파일을 지정된 경로에 저장
                ReviewImage reviewImage = reviewImageRepository.findByReview(review);

                image.transferTo(savePath.toFile());
                log.info(reviewImage);
                // 기존 객체가 있는 경우 수정
                if (reviewImage != null) {
                    String filePath = uploadPath + File.separator + reviewImage.getUuid() + "_" + reviewImage.getFileName();
                    try {
                        Files.deleteIfExists(Paths.get(filePath));
                    } catch (IOException | java.io.IOException e) {
                        log.error("Failed to delete image file :" + filePath, e);
                    }
                    reviewImage.setUuid(uuid);
                    reviewImage.setFileName(originalName);
                    // 다른 필드들도 필요에 따라 수정
                    reviewImageRepository.save(reviewImage);
                } else {
                    // 기존 객체가 없는 경우 새로 생성하여 저장
                    reviewImage = ReviewImage.builder()
                            .uuid(uuid)
                            .fileName(originalName)
                            .review(review)
                            .build();
                    reviewImageRepository.save(reviewImage);
                }
            } catch (IOException | java.io.IOException e) {
                // 파일 저장 또는 썸네일 생성 중 오류가 발생할 경우
                log.error("파일 저장하는 도중 오류가 발생했습니다: ", e);
                throw new RuntimeException("File processing error", e);
            }
        } else {
            ReviewImage reviewImage = reviewImageRepository.findByReview(review);
            if (reviewImage != null) {
                String filePath = uploadPath + File.separator + reviewImage.getUuid() + "_" + reviewImage.getFileName();
                try {
                    Files.deleteIfExists(Paths.get(filePath));
                    reviewImageRepository.deleteByReviewId(review.getReviewId());
                } catch (IOException | java.io.IOException e) {
                    log.error("Failed to delete image file :" + filePath, e);
                }
            }

        }

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
        for (ReviewImage reviewImage : result.getContent()) {
            ReviewCardDTO dto = new ReviewCardDTO();
            dto.setReviewId(reviewImage.getReview().getReviewId());
            dto.setProductColorId(reviewImage.getReview().getOrder().getProductsColorSize().getProductsColor().getProductColorId());
            byte[] image = null;
            if(reviewImage != null) {
                try {
                    image = getImage(reviewImage.getUuid(), reviewImage.getFileName());
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
                dto.setReviewImage(image);
            }

            dtoList.add(dto);
        }
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
    public PageResponseDTO<ReviewInfoDTO> readReviewList(Long productColorId, PageRequestDTO pageRequestDTO) {
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPageIndex() <= 0 ? 0 : pageRequestDTO.getPageIndex() - 1,
                pageRequestDTO.getSize(),
                Sort.by("reviewId").ascending());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Users users = userRepository.findByEmail(currentUserName).orElse(null);
        Page<Review> result = reviewRepository.findByProductColorId(productColorId, pageable);
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

        long productColorId = review.getOrder().getProductsColorSize().getProductsColor().getProductColorId();
        ReviewImage reviewImage = reviewImageRepository.findByReview(review);

        if (reviewImage != null) {
            String filePath = uploadPath + File.separator + reviewImage.getUuid() + "_" + reviewImage.getFileName();
            try {
                Files.deleteIfExists(Paths.get(filePath));
                reviewImageRepository.deleteByReviewId(review.getReviewId());
            } catch (IOException | java.io.IOException e) {
                log.error("Failed to delete image file :" + filePath, e);
            }
        }

        ProductsStar productsStar = productsStarRepository.findByProductColorId(productColorId).orElse(null);
        productsStar.setStarTotal(productsStar.getStarTotal() - review.getStar());
        reviewRepository.deleteById(reviewId);

        double starAvg = Math.round((productsStar.getStarTotal() / reviewRepository.countByProductColorId(productColorId) * 10.0) / 10.0);
        log.info(starAvg);
        productsStar.setStarAvg(starAvg);
        productsStarRepository.save(productsStar);
        }

    }
