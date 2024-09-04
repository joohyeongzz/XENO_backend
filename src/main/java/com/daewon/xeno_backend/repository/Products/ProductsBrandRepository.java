package com.daewon.xeno_backend.repository.Products;

import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsBrand;
import com.daewon.xeno_backend.domain.auth.Brand;
import com.daewon.xeno_backend.domain.auth.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductsBrandRepository extends JpaRepository<ProductsBrand, Long>{

    List<ProductsBrand> findByBrand(Brand brand);

    // 특정 제품과 관련된 모든 판매자 정보를 삭제하는 메서드
    void deleteByProducts(Products products);

    ProductsBrand findByProducts(Products products);

    @Query("SELECT p FROM ProductsBrand p where p.products.productId = :productId and p.brand = :brand")
    ProductsBrand findByProductIdAndBrand(Long productId, Brand brand);

}
