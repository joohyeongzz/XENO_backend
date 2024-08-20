package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.Users;
import com.daewon.xeno_backend.dto.auth.AuthSignupDTO;
import com.daewon.xeno_backend.dto.auth.SellerInfoCardDTO;
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
}
