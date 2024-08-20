package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductsColorRepository extends JpaRepository<ProductsColor, Long> {
    @Query("SELECT p FROM ProductsColor p WHERE p.products.productId = :productId")
    List<ProductsColor> findByProductId(@Param("productId") Long productId);

    Optional<ProductsColor> findByProductColorId(Long productColorId);

    Optional<ProductsColor> findByProducts(Products products);

    List<ProductsColor> findAllByProducts(Products products);

    Optional<ProductsColor> findByProductsAndColor(Products product, String color);

    // 상품등록한 userId를 반환하도록 하는 메서드
    @Query("select p.users.userId from ProductsSeller p where p.products.productId = :productId")
    Optional<Long> findAuthorUserIdByProductId(Long productId);

//    @Query("select p.products.productId.users.userId from ProductsColor p where p.productColorId = :productColorId")
//    Optional<Long> findAuthorUserIdByProductColorId(Long productColorId);
}