package com.daewon.xeno_backend.repository.Products;


import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsImage;
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


//    Optional<Long> findByProductImageId(List<MultipartFile> productImage);

    // 단일 이미지 ID로 조회
    Optional<ProductsImage> findByProductImageId(Long productImageId);

    // 또는 여러 이미지 ID로 조회
    List<ProductsImage> findByProductImageIdIn(List<Long> productImageIds);

    void deleteByProducts(Products products);

    @Query("SELECT u FROM ProductsImage u WHERE u.productNumber = :productNumber AND u.users = :users")
    ProductsImage findByProductNumberAndUsers(String productNumber, Users users);


    @Query("SELECT u FROM ProductsImage u WHERE u.users = :users")
    List<ProductsImage> findByUsers(Users users);

    @Query("SELECT u FROM ProductsImage u WHERE u.products IS NULL")
    List<ProductsImage> findImagesWithoutProductId();


    @Query("delete from ProductsImage p WHERE p.products.productId = :productId ")
    void deleteAllByproductId(Long productId);


    @Query("SELECT p FROM ProductsImage p WHERE p.products.productId = :productId")
    Page<ProductsImage> findByProductIdPaging(Long productId, Pageable pageable);

}