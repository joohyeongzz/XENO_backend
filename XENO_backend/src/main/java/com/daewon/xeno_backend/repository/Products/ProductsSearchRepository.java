package com.daewon.xeno_backend.repository.Products;

import com.daewon.xeno_backend.domain.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductsSearchRepository extends JpaRepository<Products, Long> {

    // 카테고리를 기준으로 상품을 검색
    @Query("SELECT p FROM Products p WHERE p.category = :category")
    Page<Products> findByCategory(@Param("category") String category, Pageable pageable);




    @Query("SELECT p.productId FROM Products p WHERE p.brandName LIKE %:keyword% OR p.name LIKE %:keyword% OR p.category LIKE %:keyword% OR p.categorySub LIKE %:keyword%")
    Page<Long> findProductIdsByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
