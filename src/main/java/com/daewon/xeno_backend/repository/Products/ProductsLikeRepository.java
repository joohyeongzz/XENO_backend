package com.daewon.xeno_backend.repository.Products;



import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ProductsLikeRepository extends JpaRepository<ProductsLike, Long> {

    @Query("select p from ProductsLike p where p.products.productId=:productId")
    Optional<ProductsLike> findByProductId(Long productId);

    @Query("SELECT pl FROM ProductsLike pl WHERE pl.products.productId IN :productIds")
    List<ProductsLike> findByProductIds(@Param("productIds") List<Long> productIds);


    // 특정 제품과 관련된 모든 좋아요를 삭제하는 메서드
    void deleteByProducts(Products products);
}