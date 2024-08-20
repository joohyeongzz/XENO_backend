package com.daewon.xeno_backend.utils;

import com.daewon.xeno_backend.dto.auth.AuthSigninDTO;
import com.daewon.xeno_backend.repository.ProductsColorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Log4j2
public class ProductSecurityUtils {

    private final ProductsColorRepository productsColorRepository;

    // 현재 사용자의 상품인지 확인하는 메서드
    public boolean isProductOwner(Long productId) {
        log.info("productId: " + productId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
        Long userId = authSigninDTO.getUserId();
        log.info("userId: " + userId);

        Optional<Long> authorUserIdOptional = productsColorRepository.findAuthorUserIdByProductId(productId);
        log.info("authorUseridOptional: " + authorUserIdOptional);

        if (authorUserIdOptional.isEmpty()) {
            return false;
        }

        Long productAuthorId = authorUserIdOptional.get();

        // SecurityContextHolder에 등록된 userId랑 주문한 사용자의 id 값이 맞는지 확인. 맞으면 - true, 안 맞으면 - false
        return userId.equals(productAuthorId);
    }

//    public boolean isProductColorOwner(Long productColorId) {
//        log.info("productColorId: " + productColorId);
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
//        Long userId = authSigninDTO.getUserId();
//        log.info("userId: " + userId);
//
//        Optional<Long> authorUserIdOptional = productsColorRepository.
//    }
}
