
package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.dto.auth.SellerInfoCardDTO;
import com.daewon.xeno_backend.exception.UserNotFoundException;
import com.daewon.xeno_backend.security.UsersDetailsService;
import com.daewon.xeno_backend.service.AuthService;
import com.daewon.xeno_backend.utils.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final PlatformTransactionManager transactionManager;

//    @DeleteMapping("/delete")
//    public ResponseEntity<?> deleteBrand(Authentication authentication) {
//
//        log.info("브랜드 삭제 요청 받음. 이메일: {}", authentication.getName());
//        try {
//            String userEmail = authentication.getName();
//            authService.deleteBrand(userEmail);
//            log.info("브랜드 삭제 성공. 이메일: {}", userEmail);
//            return ResponseEntity.ok("브랜드 계정 및 관련 데이터가 성공적으로 삭제되었습니다.");
//        } catch (UserNotFoundException e) {
//            log.warn("브랜드 삭제 실패: 사용자를 찾을 수 없음. 이메일: {}", authentication.getName());
//            return ResponseEntity.notFound().build();
//        } catch (IllegalStateException e) {
//            log.warn("브랜드 삭제 실패: {}. 이메일: {}", e.getMessage(), authentication.getName());
//            return ResponseEntity.badRequest().body(e.getMessage());
//        } catch (Exception e) {
//            log.error("브랜드 삭제 중 예기치 않은 오류 발생. 이메일: {}", authentication.getName(), e);
//            return ResponseEntity.internalServerError().body("브랜드 삭제 중 오류가 발생했습니다.");
//        }
//    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteBrand(Authentication authentication) {
        log.info("브랜드 삭제 요청 받음. 이메일: {}", authentication.getName());
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        try {
            transactionTemplate.execute(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction(TransactionStatus status) {
                    try {
                        authService.deleteBrand(authentication.getName());
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            });
            log.info("브랜드 삭제 성공. 이메일: {}", authentication.getName());
            return ResponseEntity.ok("브랜드 계정 및 관련 데이터가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            log.error("브랜드 삭제 중 예기치 않은 오류 발생. 이메일: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError().body("브랜드 삭제 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/read")
    public ResponseEntity<?> readSellerInfo(@AuthenticationPrincipal UserDetails userDetails) {

        SellerInfoCardDTO dto = authService.readSellerInfo(userDetails);
        return ResponseEntity.ok(dto);


    }

}

