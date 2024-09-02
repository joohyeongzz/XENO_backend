package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.domain.auth.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    List<Cart> findByUser(Users user);

    void deleteByUserAndProductsOption(Users user, ProductsOption productsOption);

    @Query("SELECT c FROM Cart c WHERE c.productsOption.productOptionId= :productOptionId and c.user.userId = :userId")
    Optional<Cart> findByProductOptionIdAndUser(Long productOptionId, Long userId);

    Optional<Cart> findByCartIdAndUserUserId(Long cartId, Long userId);

    @Query("select c.user.userId from Cart c where c.cartId = :cartId")
    Optional<Long> findAuthorUserIdByCartId(Long cartId);
}