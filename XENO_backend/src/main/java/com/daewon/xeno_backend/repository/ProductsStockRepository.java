package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.ProductsColorSize;
import com.daewon.xeno_backend.domain.ProductsStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductsStockRepository extends JpaRepository<ProductsStock, Long> {

    @Query("select p from ProductsStock p where p.productsColorSize.productColorSizeId = :productColorSizeId")
    ProductsStock findByProductColorSizeId(@Param("productColorSizeId") Long productColorSizeId);

    Optional<ProductsStock> findByProductsColorSize(ProductsColorSize productsColorSize);
}