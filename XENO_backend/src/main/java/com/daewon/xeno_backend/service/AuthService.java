package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.auth.AuthSignupDTO;
import com.daewon.xeno_backend.dto.auth.BrandDTO;
import com.daewon.xeno_backend.dto.auth.SellerInfoCardDTO;
import com.daewon.xeno_backend.dto.auth.TokenDTO;
import com.daewon.xeno_backend.dto.signup.UserRegisterDTO;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {

    // 이메일 중복 검사 예외 Exception
    static class UserEmailExistException extends Exception {

        public UserEmailExistException() {}
        public UserEmailExistException(String msg) {
            super(msg);
        }
    }

    Users signup(AuthSignupDTO authSignupDTO) throws UserEmailExistException;

    Users signupSeller(AuthSignupDTO authSignupDTO) throws UserEmailExistException;

    Users signin(final String email, final String password);

    SellerInfoCardDTO readSellerInfo(UserDetails userDetails);

    UserRegisterDTO registerBrandUser(BrandDTO dto);

    // refreshToken 토큰 검증 및  accessToken 재발급 받는 메서드
    TokenDTO tokenReissue(String refreshToken);
}
