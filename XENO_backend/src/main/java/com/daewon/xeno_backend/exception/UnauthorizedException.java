package com.daewon.xeno_backend.exception;

// 권한이 없는 사용자를 식별하는 Exception
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
