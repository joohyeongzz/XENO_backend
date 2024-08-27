package com.daewon.xeno_backend.repository;



import com.daewon.xeno_backend.domain.ProductsLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;


public interface ProductsLikeRepository extends JpaRepository<ProductsLike, Long> {

    @Query("select p from ProductsLike p where p.products.productId=:productId")
    Optional<ProductsLike> findByProductId(Long productId);
}