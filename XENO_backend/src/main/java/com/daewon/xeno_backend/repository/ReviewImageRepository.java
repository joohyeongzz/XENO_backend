package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.Review;
import com.daewon.xeno_backend.domain.ReviewImage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    ReviewImage findByReview(Review review);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReviewImage r WHERE r.review.reviewId = :reviewId")
    void deleteByReviewId(Long reviewId);

}