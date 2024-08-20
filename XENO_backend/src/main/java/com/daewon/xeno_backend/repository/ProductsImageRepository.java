package com.daewon.xeno_backend.repository;


import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductsImageRepository extends JpaRepository<ProductsImage, Long> {

    @Query("SELECT p FROM ProductsImage p WHERE p.productsColor.productColorId = :productColorId")
    List<ProductsImage> findByProductColorId(@Param("productColorId") Long productColorId);


    @Query("SELECT p FROM ProductsImage p WHERE p.productsColor.productColorId = :productColorId and p.isMain=true")
    ProductsImage findFirstByProductColorId(@Param("productColorId") Long productColorId);

    ProductsImage findByProductsColorProductColorIdAndIsMainTrue(Long productColorId);
//
//    Optional<Long> findByProductImageId(List<MultipartFile> productImage);

    // 단일 이미지 ID로 조회
    Optional<ProductsImage> findByProductImageId(Long productImageId);

    // 또는 여러 이미지 ID로 조회
    List<ProductsImage> findByProductImageIdIn(List<Long> productImageIds);

    void deleteByProductsColorProducts(Products products);




    @Query("delete from ProductsImage p WHERE p.productsColor.productColorId = :productColorId ")
    void deleteAllByProductColorId(Long productColorId);

}