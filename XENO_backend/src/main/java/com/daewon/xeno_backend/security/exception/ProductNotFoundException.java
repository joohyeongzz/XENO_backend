package com.daewon.xeno_backend.security.exception;

import jakarta.persistence.EntityNotFoundException;

public class ProductNotFoundException extends EntityNotFoundException {
    public ProductNotFoundException() {
        super("유효하지 않은 상품 아이디입니다.");
    }
}
