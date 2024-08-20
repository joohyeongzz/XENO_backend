package com.daewon.xeno_backend.utils;

import com.daewon.xeno_backend.dto.auth.AuthSigninDTO;
import com.daewon.xeno_backend.repository.CartRepository;
import com.daewon.xeno_backend.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 커스텀 보안 표현식을 추가하여 장바구니, 주문 유저가 맞는지 확인
@Component
@RequiredArgsConstructor
@Log4j2
public class CartAndOrderSecurityUtils {

    private final CartRepository cartRepository;
    private final OrdersRepository ordersRepository;

    // 현재 사용자가 장바구니 주인인지 확인하는 메서드
    public boolean isCartOwner(Long cartId) {
        // 현재 인증된 사용자의 정보를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // UserId값을 빼옴
        AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
        log.info("authSigninDTO: " + authSigninDTO);
        Long userId = authSigninDTO.getUserId();

        log.info("userId: " + userId);

        // 장바구니 작성자의 ID를 가져와 현재 인증된 사용자와 비교
        // cartId 기준으로 리뷰작성자의 userId를 DB에서 찾는 쿼리를 실행
        // 값이 있을 수도 있고 없을 수도 있는 상황을 표현하기 위해 Optional 사용
        Optional<Long> authorUseridOptional = cartRepository.findAuthorUserIdByCartId(cartId);
        log.info("authorUseridOptional: " + authorUseridOptional);

        if (authorUseridOptional.isEmpty()) {
            return false;
        }

        // 리뷰작성한 사용자의 id값을 가져옴
        Long cartAuthorId = authorUseridOptional.get();

        // SecurityContextHolder에 등록된 userId랑 리뷰작성한 사용자의 id 값이 맞는지 확인. 맞으면 - true, 안 맞으면 - false
        return userId.equals(cartAuthorId);
    }

    // 현재 사용자가 주문한 유저인지 확인하는 메서드
    public boolean isOrderOwner(Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
        Long userId = authSigninDTO.getUserId();
        log.info("userId: " + userId);

        Optional<Long> authorUseridOptional = ordersRepository.findAuthorUserIdByOrderId(orderId);
        log.info("authorUseridOptional: " + authorUseridOptional);

        if (authorUseridOptional.isEmpty()) {
            return false;
        }

        Long orderAuthorId = authorUseridOptional.get();

        // SecurityContextHolder에 등록된 userId랑 주문한 사용자의 id 값이 맞는지 확인. 맞으면 - true, 안 맞으면 - false
        return userId.equals(orderAuthorId);
    }

}
