package com.daewon.xeno_backend.repository;


import com.daewon.xeno_backend.domain.ProductsDetailImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductsDetailImageRepository extends JpaRepository<ProductsDetailImage, Long> {
    @Query("SELECT p FROM ProductsDetailImage p WHERE p.products.productId = :productId")
    Page<ProductsDetailImage> findByProductId(Long productId, Pageable pageable);

    @Query("SELECT p FROM ProductsDetailImage p WHERE p.products.productId = :productId")
    ProductsDetailImage findOneByproductId(Long productId);

    @Query("delete from ProductsDetailImage p WHERE p.products.productId = :productId ")
    void deleteAllByproductId(Long productId);

}
