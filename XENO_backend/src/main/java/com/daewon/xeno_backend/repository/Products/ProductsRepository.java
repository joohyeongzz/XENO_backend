package com.daewon.xeno_backend.repository.Products;


import com.daewon.xeno_backend.domain.Products;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ProductsRepository extends JpaRepository<Products, Long>{

    @Query("SELECT p FROM Products p WHERE p.category = :category")
    List<Products> findByCategory(String category);

    @Query("SELECT p FROM Products p WHERE p.category = :category and p.categorySub = :categorySub")
    List<Products> findByCategorySub(String category,String categorySub);


    @Query("SELECT p FROM Products p WHERE p.productNumber = :productNumber")
    Products findByProductNumber(@Param("productNumber") String productNumber);

    @Query("SELECT p FROM Products p WHERE p.productId = :productId")
    Products findByProductId(@Param("productId") long productId);



}