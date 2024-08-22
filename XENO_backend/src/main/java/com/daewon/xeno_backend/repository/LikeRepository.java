package com.daewon.xeno_backend.repository;




import com.daewon.xeno_backend.domain.LikeProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;


public interface LikeRepository extends JpaRepository<LikeProducts, Long> {

    @Query("SELECT l FROM LikeProducts l WHERE l.productsLike.productsColor.productColorId = :productColorId and l.users.userId = :userId")
    LikeProducts findByProductColorIdAndUserId(@Param("productColorId") Long productColorId, @Param("userId") Long userId);

    @Query("SELECT l FROM LikeProducts l WHERE l.users.userId = :userId")
    List<LikeProducts> findByUserId(@Param("userId") Long userId);
}