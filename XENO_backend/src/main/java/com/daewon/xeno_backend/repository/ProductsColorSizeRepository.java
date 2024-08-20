package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.ProductsColorSize;
import com.daewon.xeno_backend.domain.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductsColorSizeRepository extends JpaRepository<ProductsColorSize, Long> {
    @Query("SELECT p FROM ProductsColorSize p WHERE p.productsColor.productColorId = :productColorId")
    List<ProductsColorSize> findByProductColorId(@Param("productColorId") Long productColorId);


    @Query("SELECT p FROM ProductsColorSize p WHERE p.productsColor.productColorId = :productColorId and p.size = :size ")
    ProductsColorSize findByProductColorIdAndSize(@Param("productColorId") Long productColorId, Size size);

}
