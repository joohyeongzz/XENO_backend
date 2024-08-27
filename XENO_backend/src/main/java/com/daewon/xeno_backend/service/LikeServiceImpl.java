package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.repository.*;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
@Log4j2
@RequiredArgsConstructor
public class LikeServiceImpl  implements LikeService {


    private final ProductsRepository productsRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final ProductsLikeRepository productsLikeRepository;
    private final ProductsColorRepository productsColorRepository;


    @Override
    public void likeProduct(Long productColorId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserName = authentication.getName();
        log.info("이름:"+currentUserName);
        Users users = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));

        Long userId = users.getUserId();

        ProductsColor productsColor = productsColorRepository.findById(productColorId)
                .orElse(null);

        ProductsLike productLike = productsLikeRepository.findByProductColorId(productColorId)
                .orElseGet(() -> {
                    ProductsLike newProductsLike = ProductsLike.builder()
                            .productsColor(productsColor)
                            .build();
                    return productsLikeRepository.save(newProductsLike);
                });

        if (likeRepository.findByProductColorIdAndUserId(productColorId, userId) == null) {
            productLike.setLikeIndex(productLike.getLikeIndex() + 1);
            productsLikeRepository.save(productLike);
            LikeProducts likeProducts = new LikeProducts(productLike, users);
            likeRepository.save(likeProducts);
            log.info("즐겨찾기");
        } else {
            LikeProducts likeProducts = likeRepository.findByProductColorIdAndUserId(productColorId, userId);
            likeRepository.delete(likeProducts);
            productLike.setLikeIndex(productLike.getLikeIndex() - 1);
            if(productLike.getLikeIndex() == 0){
                productsLikeRepository.delete(productLike);
            } else {
                productsLikeRepository.save(productLike);
            }

            log.info("즐겨찾기 취소");
        }

    }
}
