package com.daewon.xeno_backend.repository;


import com.daewon.xeno_backend.domain.ProductsStar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ProductsStarRepository extends JpaRepository<ProductsStar, Long> {

    @Query("select p from ProductsStar p where p.productsColor.productColorId=:productColorId")
    Optional<ProductsStar> findByProductColorId(Long productColorId);

    @Query("select p from ProductsStar p where p.productsColor.productColorId=:productId")
    Optional<ProductsStar> findByProductId(Long productId);

    @Query("SELECT ps FROM ProductsStar ps JOIN ps.productsColor pc JOIN pc.products p WHERE p.category = :category ORDER BY ps.starAvg DESC")
    Page<ProductsStar> findByStarAvgDesc(@Param("category") String category, Pageable pageable);

    @Query("SELECT ps FROM ProductsStar ps JOIN ps.productsColor pc JOIN pc.products p WHERE p.category = :category ORDER BY ps.starAvg DESC")
    List<ProductsStar> findByTop10StarAvgDesc(@Param("category") String category);

}