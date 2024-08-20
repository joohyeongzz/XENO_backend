package com.daewon.xeno_backend.utils;

import com.daewon.xeno_backend.dto.auth.AuthSigninDTO;
import com.daewon.xeno_backend.repository.ReplyRepository;
import com.daewon.xeno_backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 커스텀 보안 표현식을 추가하여 리뷰, 댓글 작성자가 맞는지 확인
@Component
@RequiredArgsConstructor
@Log4j2
public class ReviewAndReplySecurityUtils {

    private final ReviewRepository reviewRepository;
    private final ReplyRepository replyRepository;

    // 현재 사용자가 리뷰의 작성자인지 확인하는 메서드
    public boolean isReviewOwner(Long reviewId) {
        // 현재 인증된 사용자의 정보를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // UserId값을 빼옴
        AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
        log.info("authSigninDTO: " + authSigninDTO);
        Long userId = authSigninDTO.getUserId();

        log.info("userId: " + userId);

        // 리뷰 작성자의 ID를 가져와 현재 인증된 사용자와 비교
        // reviewId 기준으로 리뷰작성자의 userId를 DB에서 찾는 쿼리를 실행
        // 값이 있을 수도 있고 없을 수도 있는 상황을 표현하기 위해 Optional 사용
        Optional<Long> authorUseridOptional = reviewRepository.findAuthorUserIdById(reviewId);
        log.info("authorUseridOptional: " + authorUseridOptional);

        if (authorUseridOptional.isEmpty()) {
            return false;
        }

        // 리뷰작성한 사용자의 id값을 가져옴
        Long reviewAuthorId = authorUseridOptional.get();

        // SecurityContextHolder에 등록된 userId랑 리뷰작성한 사용자의 id 값이 맞는지 확인. 맞으면 - true, 안 맞으면 - false
        return userId.equals(reviewAuthorId);

    }

    // 현재 사용자가 댓글의 작성자인지 확인하는 메서드
    public boolean isReplyOwner(Long replyId) {
        // 현재 인증된 사용자의 정보를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // UserId값을 빼옴
        AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
        log.info("authSigninDTO :" + authSigninDTO);
        Long userId = authSigninDTO.getUserId();

        log.info("userId :"+userId);

        // 댓글 작성자의 ID를 가져와 현재 인증된 사용자와 비교
        // replyId 기준으로 댓글작성자의 userId를 DB에서 찾는 쿼리를 실행
        // 값이 있을 수도 있고 없을 수도 있는 상황을 표현하기 위해 Optional 사용
        Optional<Long> authorUserIdOptional = replyRepository.findAuthorUserIdById(replyId);
        log.info("authorUserIdOptional :"+authorUserIdOptional);

        if (authorUserIdOptional.isEmpty()) {
            return false;
        }

        // 댓글 작성한 사용자의 id값을 가져옴
        Long replyAuthorId = authorUserIdOptional.get();

        // SecurityContextHolder에 등록된 userId랑 댓글 작성한 사용자의 id 값이 맞는지 확인. 맞으면 - true, 안 맞으면 - false
        return userId.equals(replyAuthorId);
    }
}
