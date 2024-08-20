package com.daewon.xeno_backend;

import com.daewon.xeno_backend.dto.auth.AuthSigninDTO;
import com.daewon.xeno_backend.repository.ProductsColorRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@SpringBootTest
@Log4j2
public class AuthTest {

    @Autowired
    private ProductsColorRepository productsColorRepository;

    public boolean isProductOwner(Long productId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
        Long userId = authSigninDTO.getUserId();
        log.info("userId: " + userId);

        Optional<Long> authorUseridOptional = productsColorRepository.findAuthorUserIdByProductId(productId);
        log.info("authorUseridOptional: " + authorUseridOptional);

        if (authorUseridOptional.isEmpty()) {
            return false;
        }

        Long productAuthorId = authorUseridOptional.get();

        // SecurityContextHolder에 등록된 userId랑 주문한 사용자의 id 값이 맞는지 확인. 맞으면 - true, 안 맞으면 - false
        return userId.equals(productAuthorId);
    }
}
