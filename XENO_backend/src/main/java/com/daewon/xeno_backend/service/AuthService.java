package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.auth.Manager;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.auth.*;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {

    // 이메일 중복 검사 예외 Exception
    static class UserEmailExistException extends Exception {

        public UserEmailExistException() {}
        public UserEmailExistException(String msg) {
            super(msg);
        }
    }

    Users signup(UserSignupDTO userSignupDTO) throws UserEmailExistException;

    Users signin(final String email, final String password);

    SellerInfoCardDTO readSellerInfo(UserDetails userDetails);

    UserSignupDTO signupBrand(BrandDTO dto);

    Manager signupManager(UserSignupDTO userSignupDTO);

    // refreshToken 토큰 검증 및  accessToken 재발급 받는 메서드
    TokenDTO tokenReissue(String refreshToken);
}
