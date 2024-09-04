package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.dto.UploadImageReadDTO;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.product.*;
import com.daewon.xeno_backend.service.ExcelService;
import com.daewon.xeno_backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;
    private final ExcelService excelService;

    @GetMapping("/read")
    public ResponseEntity<ProductInfoDTO> readProduct(@RequestParam("productId") Long productId) throws IOException {
        log.info(productId);
        ProductInfoDTO productInfoDTO = productService.getProductInfo(productId);

        return ResponseEntity.ok(productInfoDTO);
    }

    @GetMapping("/read-by-brand")
    public ResponseEntity<ProductInfoDTO> readProductByBrand(@RequestParam("productId") Long productId) throws IOException {
        log.info(productId);
        ProductInfoDTO productInfoDTO = productService.getProductInfoByBrand(productId);

        return ResponseEntity.ok(productInfoDTO);
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
    @GetMapping("/read/card")
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


    @GetMapping("/brand/read")
    public ResponseEntity<?> getProductListByBrand(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userEmail = userDetails.getUsername();

            log.info("orderUserEmail : " + userEmail);
            List<ProductListByBrandDTO> dtoList = productService.getProductListByBrand(userEmail);

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

             productService.uploadImages(productNumber, productImages != null && !productImages.isEmpty() ? productImages : null,
                    productDetailImage != null && !productDetailImage.isEmpty() ? productDetailImage : null
            );
            return ResponseEntity.ok("\"성공\"");
        } catch (Exception e) {
            log.error("상품 등록 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/update/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProductImages(
            @RequestPart("productNumber") String productNumber,
            @RequestPart(name = "productImages")  List<MultipartFile> productImages,
            @RequestPart(name = "productDetailImage") MultipartFile productDetailImage) {
        try {

            productService.updateProductImages(productNumber, productImages != null && !productImages.isEmpty() ? productImages : null,
                    productDetailImage != null && !productDetailImage.isEmpty() ? productDetailImage : null
            );
            return ResponseEntity.ok("\"성공\"");
        } catch (Exception e) {
            log.error("상품 등록 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "업로드 이미지 확인")
    @GetMapping("/read/all-upload-images")
    public ResponseEntity<List<UploadImageReadDTO>> getUploadImageAll() {
        List<UploadImageReadDTO> dto = productService.getUploadImageAll();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "품번에 따른 업로드 이미지 확인")
    @GetMapping("/read/upload-image")
    public ResponseEntity<UploadImageReadDTO> getUploadImageByProductNumber(@RequestParam String productNumber) {
        UploadImageReadDTO dto = productService.getUploadImageByProductNumber(productNumber);
        return ResponseEntity.ok(dto);
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @RequestPart(name = "excel") MultipartFile excel) {
        productService.saveProductsFromExcel(excel);
        return ResponseEntity.ok("\"성공\"");
    }

    @PutMapping(value = "/update/stock", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProductStock(@RequestPart(name = "excel") MultipartFile excel) {
        // Check if the file is empty
        if (excel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("엑셀 파일이 업로드되지 않았습니다.");
        }

        // Validate file type
        String contentType = excel.getContentType();
        if (contentType == null || !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("유효하지 않은 파일 형식입니다. 엑셀 파일만 업로드할 수 있습니다.");
        }

        try {
            // Process the file
            excelService.parseStockExcelFile(excel);
            return ResponseEntity.ok("\"성공\"");
        } catch (IOException e) {
            // Log the exception and return a server error response
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 처리 중 오류가 발생했습니다.");
        } catch (Exception e) {
            // Handle other exceptions
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("예상치 못한 오류가 발생했습니다.");
        }
    }






    @Operation(summary = "엑셀 다운로드")
    @GetMapping("/download/excel")
    public void download(HttpServletResponse response) throws IOException {
        byte[] excelFile = excelService.generateExcelFile();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=productList.xlsx");
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(excelFile);
            outputStream.flush();
        }
    }

    @Operation(summary = "새 엑셀 다운로드")
    @GetMapping("/download/new-excel")
    public void downloadNewExcel(HttpServletResponse response) throws IOException {
        byte[] excelFile = excelService.newGenerateExcelFile();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=productList.xlsx");
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(excelFile);
            outputStream.flush();
        }
    }


    @Operation(summary = "재고 엑셀 다운로드")
    @GetMapping("/download/stock-excel")
    public void downloadStockExcel(HttpServletResponse response) throws IOException {
        byte[] excelFile = excelService.generateStockExcelFile();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=productStock.xlsx");
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(excelFile);
            outputStream.flush();
        }
    }


}


