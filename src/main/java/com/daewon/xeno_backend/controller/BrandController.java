
package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.dto.auth.BrandInfoCardDTO;
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

import java.util.HashMap;
import java.util.Map;

@Log4j2
@RequestMapping("/api/brand")
@RequiredArgsConstructor
@RestController
@EnableWebSecurity
public class BrandController {

    private final JWTUtil jwtUtil;
    private final AuthService authService;
    private final UsersDetailsService usersDetailsService;
    private final PlatformTransactionManager transactionManager;

    // brand 탈퇴 메서드
//    @PreAuthorize("hasRole('SELLER, MANAGER')")
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String,String>> deleteBrand(Authentication authentication) {
        log.info("브랜드 삭제 요청 받음. 이메일: {}", authentication.getName());

        // TransactionalTemplate을 생성하여 프로그래밍 방식의 Transactional 관리를 가능하게 함
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {
            // execute() 메서드를 사용하여 trasactional 내에서 코드 블록 실행
            transactionTemplate.execute(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction(TransactionStatus status) {
                    try {
                        authService.deleteBrand(authentication.getName());
                    } catch (Exception e) {
                        // 예외 발생 시 transactional을 롤백하도록 표시
                        status.setRollbackOnly();
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            });
            log.info("브랜드 삭제 성공. 이메일: {}", authentication.getName());

            Map<String, String> response = new HashMap<>();
            response.put("message", "브랜드 계정 및 관련 데이터가 성공적으로 삭제되었습니다.");
            response.put("deleteBrand", authentication.toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("브랜드 삭제 중 예기치 않은 오류 발생. 이메일: {}", authentication.getName(), e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "브랜드 삭제 중 오류가 발생했습니다." + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/read")
    public ResponseEntity<?> readBrandInfo(@AuthenticationPrincipal UserDetails userDetails) {
        BrandInfoCardDTO dto = authService.readBrandInfo(userDetails);
        return ResponseEntity.ok(dto);
    }

}

