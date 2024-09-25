package com.daewon.xeno_backend.repository.Products;


import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsStar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ProductsStarRepository extends JpaRepository<ProductsStar, Long> {

    @Query("select p from ProductsStar p where p.products.productId=:productId")
    Optional<ProductsStar> findByProductId(Long productId);

    @Query("SELECT ps FROM ProductsStar ps WHERE ps.products.productId IN :productIds")
    List<ProductsStar> findByProductIds(@Param("productIds") List<Long> productIds);

    @Query("SELECT ps FROM ProductsStar ps JOIN ps.products p WHERE p.category = :category ORDER BY ps.starAvg DESC")
    Page<ProductsStar> findByStarAvgDesc(@Param("category") String category, Pageable pageable);

    @Query("SELECT ps FROM ProductsStar ps JOIN ps.products p WHERE p.category = :category ORDER BY ps.starAvg DESC")
    List<ProductsStar> findByTop10StarAvgDesc(@Param("category") String category);

    // 특정 제품과 관련된 모든 별점을 삭제하는 메서드
    void deleteByProducts(Products products);

}