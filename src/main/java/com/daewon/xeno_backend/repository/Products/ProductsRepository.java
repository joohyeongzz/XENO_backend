package com.daewon.xeno_backend.repository.Products;


import com.daewon.xeno_backend.domain.Products;

import com.daewon.xeno_backend.domain.auth.Brand;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ProductsRepository extends JpaRepository<Products, Long>{

    @Query("SELECT p FROM Products p WHERE p.category = :category")
    List<Products> findByCategory(String category);

    @Query("SELECT p FROM Products p " +
            "LEFT JOIN FETCH LikeProducts lp ON lp.productsLike.products.productId = p.productId " +
            "LEFT JOIN FETCH ProductsLike pl ON pl.products.productId = p.productId " +
            "LEFT JOIN FETCH ProductsStar ps ON ps.products.productId = p.productId " +
            "LEFT JOIN FETCH ProductsImage pi ON pi.products.productId = p.productId " +
            "WHERE p.category = :category")
    List<Products> findByCategoryWithDetails(String category);

    @Query("SELECT p FROM Products p WHERE p.category = :category and p.categorySub = :categorySub")
    List<Products> findByCategorySub(String category,String categorySub);


    @Query("SELECT p FROM Products p WHERE p.productNumber = :productNumber")
    Products findByProductNumber(@Param("productNumber") String productNumber);

    @Query("SELECT p FROM Products p WHERE p.productId = :productId")
    Products findByProductId(@Param("productId") long productId);



    @Query("SELECT p FROM Products p " +
            "LEFT JOIN FETCH p.image i " +
            "LEFT JOIN FETCH p.star s " +
            "LEFT JOIN FETCH p.like l " +
            "WHERE p.productId = :productId")
    Products findProductWithDetailsById(@Param("productId") Long productId);

    // 특정 브랜드 이름으로 모든 제품을 찾는 메서드
    List<Products> findByBrandName(String brandName);


    @Query("SELECT p, l, ps, pl, i FROM Products p " +
            "LEFT JOIN LikeProducts l ON p.productId = l.productsLike.products.productId " +
            "LEFT JOIN ProductsStar ps ON p.productId = ps.products.productId " +
            "LEFT JOIN ProductsLike pl ON p.productId = pl.products.productId " +
            "LEFT JOIN ProductsImage i ON p.productId = i.products.productId " +
            "WHERE p.productId = :productId")
    Object[] findProductWithDetails(@Param("productId") Long productId);


}