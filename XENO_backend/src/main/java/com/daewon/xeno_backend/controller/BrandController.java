package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.dto.auth.BrandDTO;
import com.daewon.xeno_backend.dto.signup.UserRegisterDTO;
import com.daewon.xeno_backend.service.BrandService;
import com.daewon.xeno_backend.service.BrandService2;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sellerRegister")
@RequiredArgsConstructor
@Log4j2
public class BrandController {

    private final BrandService brandService;
    private final BrandService2 brandService2;

    @PostMapping("/register/seller")
    public ResponseEntity<?> registerSeller(@RequestBody BrandDTO dto) {

        try {
            UserRegisterDTO registeredUser = brandService2.registerSellerUser(dto);
            return ResponseEntity.status(201).body("판매사 회원가입 완료");
        } catch (DataIntegrityViolationException e) {
            log.error("Email 중복 됨 : " + dto.getEmail(), e);
            return ResponseEntity.status(409).body("이미 존재하는 이메일입니다.");
        }
    }
}
