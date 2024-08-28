package com.daewon.xeno_backend.repository.Products;

import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsSeller;
import com.daewon.xeno_backend.domain.auth.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductsSellerRepository extends JpaRepository<ProductsSeller, Long>{

    List<ProductsSeller> findByUsers(Users users);

    // 특정 제품과 관련된 모든 판매자 정보를 삭제하는 메서드
    void deleteByProducts(Products products);


}
