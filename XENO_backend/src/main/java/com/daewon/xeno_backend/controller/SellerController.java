
package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.dto.auth.SellerInfoCardDTO;
import com.daewon.xeno_backend.security.UsersDetailsService;
import com.daewon.xeno_backend.service.AuthService;
import com.daewon.xeno_backend.utils.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RequestMapping("/api/seller")
@RequiredArgsConstructor
@RestController
@EnableWebSecurity
public class SellerController {

    private final JWTUtil jwtUtil;
    private final AuthService authService;
    private final UsersDetailsService usersDetailsService;



    @GetMapping("/read")
    public ResponseEntity<?> readSellerInfo(@AuthenticationPrincipal UserDetails userDetails) {

        SellerInfoCardDTO dto = authService.readSellerInfo(userDetails);
        return ResponseEntity.ok(dto);


    }

}

