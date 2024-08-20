package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    @Query("select r from Reply r where r.review.reviewId = :reviewId")
    List<Reply> listOfReview(Long reviewId);

    // 댓글 작성자의 userId를 반환하도록 하는 메서드
    @Query("select r.users.userId from Reply r where r.replyId = :replyId")
    Optional<Long> findAuthorUserIdById(Long replyId);

    // 사용자 ID로 작성한 댓글을 조회하는 메서드
    @Query("select r from Reply r where r.users.userId = :userId")
    List<Reply> findByUserId(Long userId);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.review.reviewId = :reviewId")
    int countByReviewId(@Param("reviewId") Long reviewId);

}
