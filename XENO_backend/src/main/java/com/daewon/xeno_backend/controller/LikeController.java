 package com.daewon.xeno_backend.controller;


 import com.daewon.xeno_backend.repository.Products.ProductsLikeRepository;
 import com.daewon.xeno_backend.service.LikeService;
 import io.swagger.v3.oas.annotations.Operation;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.log4j.Log4j2;

 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.security.access.prepost.PreAuthorize;
 import org.springframework.web.bind.annotation.*;

 @RestController
 @Log4j2
 @RequestMapping("/api/like")
 @RequiredArgsConstructor
 public class LikeController {

     private final LikeService likeService;
     private final ProductsLikeRepository productsLikeRepository;



     @Operation(summary = "좋아요")
     @PreAuthorize("hasRole('CUSTOMER')")
     @GetMapping()
     public ResponseEntity<String> like(@RequestParam Long productId) {
         try {
         likeService.likeProduct(productId);
             return ResponseEntity.ok("\"좋아요 성공\"");
         } catch (Exception e) {
             // 오류 발생 시
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("\"좋아요 실패\"");
         }

     }




//     @Operation(summary = "자신이 즐겨찾기한 약국 목록")
//     @GetMapping("/list") // 자신이 즐겨찾기한 약국 목록 (즐겨찾기한 순 정렬)
//     public List<PharmacyEnjoyRankListDTO> enjoyedPharmaciesList(){ //
//         List<PharmacyEnjoyRankListDTO> Pharmacylist = enjoyService.enjoyedPharmaciesListByUser();
//         return Pharmacylist;
//     }

 }
