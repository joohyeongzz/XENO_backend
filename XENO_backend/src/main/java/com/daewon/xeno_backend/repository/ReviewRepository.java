package com.daewon.xeno_backend.repository;


import com.daewon.xeno_backend.domain.Orders;
import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.Review;
import com.daewon.xeno_backend.domain.auth.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r WHERE r.order.productsOption.products.productId = :productId ")
    Page<Review> findByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.order.productsOption.products.productId = :productId")
    long countByProductId(Long productId); // 리뷰 작성한 수

    // 리뷰 작성자의 userId를 반환하도록 하는 메서드
    @Query("select r.users.userId from Review r where r.reviewId = :reviewId")
    Optional<Long> findAuthorUserIdById(Long reviewId);


    @Query("select r from Review r where r.users = :users and r.order = :orders")
    Review findByUsersAndOrders(Users users, Orders orders);

//    // 특정 제품에 대한 모든 리뷰를 찾는 메서드
//    List<Review> findByProducts(Products products);

}