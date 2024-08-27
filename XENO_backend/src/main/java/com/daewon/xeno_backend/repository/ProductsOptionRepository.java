package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.ProductsOption;
import com.daewon.xeno_backend.domain.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductsOptionRepository extends JpaRepository<ProductsOption, Long> {
    @Query("SELECT p FROM ProductsOption p WHERE p.products.productId = :productId")
    List<ProductsOption> findByProductId(@Param("productId") Long productId);

}
