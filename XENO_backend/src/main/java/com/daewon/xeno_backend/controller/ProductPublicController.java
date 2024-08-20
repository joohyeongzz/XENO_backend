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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductPublicController {

    private final ProductService productService;

    // @PreAuthorize("hasRole('USER')")

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @RequestPart("productCreateDTO") String productRegisterDTOStr,
            @RequestPart("productImage") List<MultipartFile> productImage,
            @RequestPart("productDetailImage") MultipartFile productDetailImage) {

        ProductRegisterDTO productDTO;

        try {
            // JSON 문자열을 ReviewDTO 객체로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            productDTO = objectMapper.readValue(productRegisterDTOStr, ProductRegisterDTO.class);
            log.info(productDTO);
        } catch (IOException e) {
            // JSON 변환 중 오류가 발생하면 로그를 남기고 예외 발생
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON format", e);
        }
        try {
            Products createdProduct = productService.createProduct(productDTO, productImage != null && !productImage.isEmpty() ? productImage : null,
                    productDetailImage != null && !productDetailImage.isEmpty() ? productDetailImage : null
            );
            return ResponseEntity.ok("성공");
        } catch (Exception e) {
            log.error("상품 등록 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/color/read")
    public ResponseEntity<ProductInfoDTO> readProductColor(@RequestParam Long productColorId) throws IOException {
        ProductInfoDTO productInfoDTO = productService.getProductColorInfo(productColorId);

        return ResponseEntity.ok(productInfoDTO);
    }

    @GetMapping("/read")
    public ResponseEntity<ProductCreateGetInfoDTO> readProduct(@RequestParam Long productId) throws IOException {
        ProductCreateGetInfoDTO productInfoDTO = productService.getProductInfo(productId);

        return ResponseEntity.ok(productInfoDTO);
    }

    @GetMapping("/color/readImages")
    public ResponseEntity<ProductDetailImagesDTO> readProductDetailImages(
            @RequestParam Long productColorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size) {

        try {
            // ProductService를 통해 페이징 처리된 상품의 상세 이미지 가져오기
            ProductDetailImagesDTO productDetailImagesDTO = productService.getProductDetailImages(productColorId, page,
                    size);

            // 페이징된 이미지 데이터와 HTTP 200 OK 응답 반환
            return ResponseEntity.ok(productDetailImagesDTO);
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/color/readFirstImages")
    public ResponseEntity<List<ProductOtherColorImagesDTO>> readFirstProductImages(@RequestParam Long productColorId) {

        try {
            List<ProductOtherColorImagesDTO> firstProductImages = productService
                    .getRelatedColorProductsImages(productColorId);
            // 페이징된 이미지 데이터와 HTTP 200 OK 응답 반환
            return ResponseEntity.ok(firstProductImages);
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
    public ResponseEntity<ProductOrderBarDTO> readOrderBar(@RequestParam Long productColorId) {

        try {
            ProductOrderBarDTO orderBar = productService.getProductOrderBar(productColorId);
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
    public ResponseEntity<ProductColorInfoCardDTO> readProductCardInfo(@RequestParam Long productColorId) {

        try {
            ProductColorInfoCardDTO product = productService.getProductCardInfo(productColorId);
            // 페이징된 이미지 데이터와 HTTP 200 OK 응답 반환
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "top10")
    @GetMapping("/rank/{category}")
    public ResponseEntity<List<ProductsStarRankListDTO>> getranktop10(
            @PathVariable String category) {
        try {
        List<ProductsStarRankListDTO> result = productService.getranktop10(category);
        log.info(result);
        return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Operation(summary = "top50")
    @GetMapping("/rank/page/{category}")
    public ResponseEntity<PageInfinityResponseDTO<ProductsStarRankListDTO>> getrankTop50(
            PageRequestDTO pageRequestDTO,
            @PathVariable String category) {
        try {
            log.info(category);
            PageInfinityResponseDTO<ProductsStarRankListDTO> result = productService.getrankTop50(category, pageRequestDTO);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "브랜드명, 이름, 카테고리, 카테고리 sub 검색")
    @GetMapping("/search")
    public ResponseEntity<PageResponseDTO<ProductsSearchDTO>> searchProducts(
            @RequestParam String keyword,
            @ModelAttribute PageRequestDTO pageRequestDTO) {
        try {


            PageResponseDTO<ProductsSearchDTO> result = productService.BrandNameOrNameOrCategoryOrCategorysubSearch(keyword, pageRequestDTO);
            return ResponseEntity.ok(result);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
