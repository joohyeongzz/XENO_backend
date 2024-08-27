package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.product.*;
import com.daewon.xeno_backend.service.ProductService;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/read")
    public ResponseEntity<ProductInfoDTO> readProduct(@RequestParam("productId") Long productId) throws IOException {
        log.info(productId);
        ProductInfoDTO productInfoDTO = productService.getProductInfo(productId);

        return ResponseEntity.ok(productInfoDTO);
    }


    @GetMapping("/color/readImages")
    public ResponseEntity<ProductDetailImagesDTO> readProductDetailImages(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size) {

        try {
            // ProductService를 통해 페이징 처리된 상품의 상세 이미지 가져오기
            ProductDetailImagesDTO productDetailImagesDTO = productService.getProductDetailImages(productId, page,
                    size);

            // 페이징된 이미지 데이터와 HTTP 200 OK 응답 반환
            return ResponseEntity.ok(productDetailImagesDTO);
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Operation(summary = "카테고리")
    @GetMapping("/read/category")
    public ResponseEntity<List<ProductColorInfoCardDTO>> readProductsListByCategory(@RequestParam String categoryId,
                                                                                @RequestParam(required = false, defaultValue = "") String categorySubId) {

        try {
            List<ProductColorInfoCardDTO> products = productService.getProductsInfoByCategory(categoryId, categorySubId);
            // 페이징된 이미지 데이터와 HTTP 200 OK 응답 반환
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/color/readOrderBar")
    public ResponseEntity<ProductOrderBarDTO> readOrderBar(@RequestParam Long productId) {

        try {
            ProductOrderBarDTO orderBar = productService.getProductOrderBar(productId);
            // 페이징된 이미지 데이터와 HTTP 200 OK 응답 반환
            return ResponseEntity.ok(orderBar);
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "좋아요한 상품")
    @GetMapping("/read/like")
    public ResponseEntity<List<ProductColorInfoCardDTO>> readLikedProductList() {

        try {
            List<ProductColorInfoCardDTO> products = productService.getLikedProductsInfo();
            // 페이징된 이미지 데이터와 HTTP 200 OK 응답 반환
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "상품 카드")
    @GetMapping("/color/read/info")
    public ResponseEntity<ProductColorInfoCardDTO> readProductCardInfo(@RequestParam Long productId) {

        try {
            ProductColorInfoCardDTO product = productService.getProductCardInfo(productId);
            // 페이징된 이미지 데이터와 HTTP 200 OK 응답 반환
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/seller/read")
    public ResponseEntity<?> getProductListBySeller(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userEmail = userDetails.getUsername();

            log.info("orderUserEmail : " + userEmail);
            List<ProductListBySellerDTO> dtoList = productService.getProductListBySeller(userEmail);

            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("해당하는 상품 또는 재고가 없습니다.");
        }
    }

    @Operation(summary = "top10")
    @GetMapping("/rank/{category}")
    public ResponseEntity<List<ProductsStarRankListDTO>> getranktop10(
            @PathVariable String category) {
        List<ProductsStarRankListDTO> result = productService.getranktop10(category);
        log.info(result);
        return ResponseEntity.ok(result);
    }
    @Operation(summary = "top50")
    @GetMapping("/rank/page/{category}")
    public ResponseEntity<PageInfinityResponseDTO<ProductsStarRankListDTO>> getrankTop50(
            PageRequestDTO pageRequestDTO,
            @PathVariable String category) {
        log.info(category);
        PageInfinityResponseDTO<ProductsStarRankListDTO> result = productService.getrankTop50(category, pageRequestDTO);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "브랜드명, 이름 검색")
    @GetMapping("/search")
    public ResponseEntity<PageResponseDTO<ProductsSearchDTO>> searchProducts(
            @RequestParam String keyword,
            @ModelAttribute PageRequestDTO pageRequestDTO) {
        PageResponseDTO<ProductsSearchDTO> result = productService.BrandNameOrNameOrCategoryOrCategorysubSearch(keyword, pageRequestDTO);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @RequestPart("productNumber") String productNumber,
            @RequestPart(name = "productImages")  List<MultipartFile> productImages,
            @RequestPart(name = "productDetailImage") MultipartFile productDetailImage) {
        try {
            log.info("asdsad");
             productService.uploadImages(productNumber, productImages != null && !productImages.isEmpty() ? productImages : null,
                    productDetailImage != null && !productDetailImage.isEmpty() ? productDetailImage : null
            );
            return ResponseEntity.ok("\"성공\"");
        } catch (Exception e) {
            log.error("상품 등록 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @RequestPart(name = "excel") MultipartFile excel) {
        productService.saveProductsFromExcel(excel);
        return ResponseEntity.ok("\"성공\"");
    }


}