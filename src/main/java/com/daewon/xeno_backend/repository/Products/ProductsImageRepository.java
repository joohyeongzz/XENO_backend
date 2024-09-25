package com.daewon.xeno_backend.repository.Products;


import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsImage;
import com.daewon.xeno_backend.domain.auth.Brand;
import com.daewon.xeno_backend.domain.auth.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductsImageRepository extends JpaRepository<ProductsImage, Long> {

    @Query("SELECT p FROM ProductsImage p WHERE p.products.productId = :productId")
     ProductsImage findByProductId(@Param("productId") Long productId);
    @Query("SELECT pi FROM ProductsImage pi WHERE pi.products.productId IN :productIds")
    List<ProductsImage> findByProductIds(@Param("productIds") List<Long> productIds);

//    Optional<Long> findByProductImageId(List<MultipartFile> productImage);

    // 단일 이미지 ID로 조회
    Optional<ProductsImage> findByProductImageId(Long productImageId);

    // 또는 여러 이미지 ID로 조회
    List<ProductsImage> findByProductImageIdIn(List<Long> productImageIds);

    @Query("SELECT u FROM ProductsImage u WHERE u.productNumber = :productNumber AND u.brand = :brand AND u.products IS NOT NULL")
    ProductsImage findByProductNumberAndUsersAndProductsIsNotNull(String productNumber, Brand brand);

    @Query("SELECT u FROM ProductsImage u WHERE u.productNumber = :productNumber AND u.brand = :brand")
    ProductsImage findByProductNumberAndUsers(String productNumber, Brand brand);

    @Query("SELECT u FROM ProductsImage u WHERE u.brand = :brand")
    List<ProductsImage> findByUsers(Brand brand);

    @Query("SELECT p FROM ProductsImage p WHERE p.products IS NULL AND p.brand = :brand")
    List<ProductsImage> findByProductsIsNullAndUsers(Brand brand);

    @Query("SELECT u FROM ProductsImage u WHERE u.products IS NULL")
    List<ProductsImage> findImagesWithoutProductId();


    @Query("delete from ProductsImage p WHERE p.products.productId = :productId ")
    void deleteAllByproductId(Long productId);


    @Query("SELECT p FROM ProductsImage p WHERE p.products.productId = :productId")
    Page<ProductsImage> findByProductIdPaging(Long productId, Pageable pageable);

    // 특정 제품과 관련된 모든 이미지를 삭제하는 메서드
    void deleteByProducts(Products products);

}