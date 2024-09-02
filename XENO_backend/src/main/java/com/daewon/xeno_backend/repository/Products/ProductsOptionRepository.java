package com.daewon.xeno_backend.repository.Products;

import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductsOptionRepository extends JpaRepository<ProductsOption, Long> {
    @Query("SELECT p FROM ProductsOption p WHERE p.products.productId = :productId")
    List<ProductsOption> findByProductId(@Param("productId") Long productId);

    //
    void deleteByProducts_ProductId(Long productId);

    @Modifying
    @Query("DELETE FROM ProductsOption po WHERE po.products.productId = :productId")
    int deleteAllByProductId(@Param("productId") Long productId);

}
