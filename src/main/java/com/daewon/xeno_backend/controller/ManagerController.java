package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.domain.auth.Level;
import com.daewon.xeno_backend.domain.auth.UserRole;
import com.daewon.xeno_backend.dto.auth.UserSignupDTO;
import com.daewon.xeno_backend.dto.manager.*;
import com.daewon.xeno_backend.exception.*;
import com.daewon.xeno_backend.service.AuthService;
import com.daewon.xeno_backend.service.ManagerService;
import com.daewon.xeno_backend.utils.JWTUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Log4j2
public class ManagerController {

    private final AuthService authService;
    private final ManagerService managerService;

    private final JWTUtil jwtUtil;
    private final TransactionTemplate transactionTemplate;

    // user List 불러오는 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping()
    public ResponseEntity<List<UserListDTO>> getAllUsers() {
        return ResponseEntity.ok(managerService.getAllUsers());
    }

    // brand List 불러오는 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/brand")
    public ResponseEntity<List<BrandListDTO>> getAllBrands() {
        return ResponseEntity.ok(managerService.getAllBrands());
    }

    // brand 승인 대기중인 List 불러오는 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/brand/approve")
    public ResponseEntity<List<BrandApproveListDTO>> getAllBrandApproves() {
        return ResponseEntity.ok(managerService.getAllBrandApprovers());
    }

    // product List 불러오는 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/brand/products")
    public ResponseEntity<List<ProductListDTO>> getAllProducts() {
        return ResponseEntity.ok(managerService.getAllProducts());
    }

    // user 강제 탈퇴 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<?> deleteUser(Authentication authentication, @RequestHeader("Authorization") String token,
                                        @PathVariable Long targetUserId) {

        // token의 claim값에서 email값을 추출 후 문제 없으면 정상적으로 {targetUserId}값이 삭제 됨.
        try {
            String currentToken = token.replace("Bearer ", "");
            Map<String, Object> claims = jwtUtil.validateToken(currentToken);
            String managerEmail = claims.get("email").toString();
            log.info("managerEmail: " + managerEmail);

            String deletedUserEmail = managerService.deleteUserByManager(managerEmail, targetUserId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "해당 user의 탈퇴가 완료되었습니다.");
            response.put("deletedUserEmail", deletedUserEmail);

            log.info("delete중인 manager의 email은? : " + deletedUserEmail);

            return ResponseEntity.ok(response);
        } catch (JwtException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "token이 유효하지 않거나 만료됨");

            return ResponseEntity.status(401).body(errorResponse);
        } catch (UserNotFoundException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(404).body(errorResponse);
        } catch (UnauthorizedException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자를 삭제하는 도중 오류가 발생 : " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/roles/{targetUserId}")
    public ResponseEntity<String> updateUserRoles(Authentication authentication,
                                                  @RequestHeader("Authorization") String token,
                                                  @RequestBody Set<UserRole> newRole,
                                                  @PathVariable Long targetUserId) {
        try {
            String currentToken = token.replace("Bearer ", "");
            Map<String, Object> claims = jwtUtil.validateToken(currentToken);
            String managerEmail = claims.get("email").toString();

            managerService.updateUserRoleByManager(managerEmail, targetUserId, newRole);
            return ResponseEntity.ok("해당 user의 role값 변경이 완료되었습니다.");
        } catch (JwtException e) {
            log.error("JWT 토큰 처리 중 오류 발생", e);
            return ResponseEntity.status(401).body("Invalid or expired token");
        } catch (UserNotFoundException e) {
            log.warn("사용자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (UnauthorizedException e) {
            log.warn("권한 없는 작업 시도: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            log.error("사용자 역할 업데이트 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body("An error occurred while updating user roles: " + e.getMessage());
        }
    }

    // 특정 유저의 적립금 수정 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/point/{targetUserId}")
    public ResponseEntity<Map<String, Object>> updateUserPoint(Authentication authentication,
                                                               @RequestHeader("Authorization") String token,
                                                               @RequestBody PointUpdateDTO pointUpdateDto,
                                                               @PathVariable Long targetUserId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String currentToken = token.replace("Bearer ", "");
            Map<String, Object> claims = jwtUtil.validateToken(currentToken);
            String managerEmail = claims.get("email").toString();

            int newPoints = pointUpdateDto.getPointChange();

            // point 값 검증
            if (newPoints < 0) {
                throw new InvalidPointException("포인트 값은 음수일 수 없습니다.");
            }
            if (newPoints > Integer.MAX_VALUE) {
                throw new InvalidPointException("포인트 값이 허용 범위를 초과했습니다.");
            }

            managerService.updateUserPointByManager(managerEmail, targetUserId, newPoints);

            response.put("userId", targetUserId);
            response.put("newPoints", newPoints);
            response.put("message", "포인트가 성공적으로 업데이트되었습니다.");
            return ResponseEntity.ok(response);
        } catch (JwtException e) {
            log.error("JWT 토큰 처리 중 오류 발생", e);
            response.put("message", "토큰이 만료됐거나 유효하지 않음");
            return ResponseEntity.status(401).body(response);
        } catch (UserNotFoundException e) {
            log.warn("사용자를 찾을 수 없음: {}", e.getMessage());
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (UnauthorizedException e) {
            log.warn("권한 없는 작업 시도: {}", e.getMessage());
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("사용자 포인트 업데이트 중 오류 발생", e);
            response.put("message", "user point 조정 실패 : " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 특정 유저의 등급 수정 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/level/{targetUserId}")
    public ResponseEntity<Map<String, String>> updateUserLevel(
            Authentication authentication,
            @RequestHeader("Authorization") String token,
            @RequestBody LevelUpdateDTO levelUpdateDTO,
            @PathVariable Long targetUserId) {

        Map<String, String> response = new HashMap<>();

        try {
            String currentToken = token.replace("Bearer ", "");
            Map<String, Object> claims = jwtUtil.validateToken(currentToken);
            String managerEmail = claims.get("email").toString();

            String newLevelString = levelUpdateDTO.getNewLevel();
            Level newLevel;

            try {
                newLevel = Level.valueOf(newLevelString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 Level 값이 입력됨 : {}", newLevelString);
                response.put("message", "잘못된 level 값입니다. 허용된 값: " + Arrays.toString(Level.values()));
                return ResponseEntity.status(400).body(response);
            }

            managerService.updateUserLevelByManager(managerEmail, targetUserId, newLevel);
            response.put("message", "Manager에 의해 user의 level이 정상적으로 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (JwtException e) {
            log.error("JWT 토큰 처리 중 오류 발생", e);
            response.put("message", "토큰이 만료됐거나 유효하지 않음");
            return ResponseEntity.status(401).body(response);
        } catch (UserNotFoundException e) {
            log.warn("사용자를 찾을 수 없음: {}", e.getMessage());
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (UnauthorizedException e) {
            log.warn("권한 없는 작업 시도: {}", e.getMessage());
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("사용자 level 업데이트 중 오류 발생", e);
            response.put("message", "user level 변경 실패 : " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 특정 판매사 강제 탈퇴 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/brand/{targetBrandId}")
    public ResponseEntity<Map<String, Object>> deleteBrand(Authentication authentication,
                                                           @PathVariable Long targetBrandId) {
        Map<String, Object> response = new HashMap<>();

        try {
            String deleteBrandResult = transactionTemplate.execute(new TransactionCallback<String>() {
                @Override
                public String doInTransaction(TransactionStatus status) {
                    try {
                        return managerService.deleteBrandByManager(authentication.getName(), targetBrandId, null);
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        throw new RuntimeException(e);
                    }
                }
            });

            log.info("브랜드 삭제 성공. 관리자 이메일: {}, 브랜드 ID: {}", authentication.getName(), targetBrandId);
            response.put("status", "success");
            response.put("message", "브랜드가 성공적으로 삭제되었습니다.");
            response.put("data", deleteBrandResult);

            return ResponseEntity.ok(response);
        } catch (BrandNotFoundException e) {
            log.error("브랜드 not found. 브랜드 ID: {}", targetBrandId);
            response.put("status", "error");
            response.put("message", "해당하는 브랜드를 찾을 수 없습니다: " + e.getMessage());

            return ResponseEntity.status(404).body(response);
        } catch (UnauthorizedException e) {
            response.put("status", "error");
            response.put("message", "권한이 없습니다: " + e.getMessage());

            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("브랜드 삭제중 예기치 못한 오류 발생. 브랜드 ID: {}", targetBrandId, e);
            response.put("status", "error");
            response.put("message", "브랜드 삭제 중 예기치 않은 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    // 특정 판매사 유저 강제 탈퇴 메서드
    // user 강제 탈퇴 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/brand/users/{targetUserId}")
    public ResponseEntity<?> deleteBrandDependsUser(Authentication authentication, @RequestHeader("Authorization") String token,
                                        @PathVariable Long targetUserId) {

        // token의 claim값에서 email값을 추출 후 문제 없으면 정상적으로 {targetUserId}값이 삭제 됨.
        try {
            String currentToken = token.replace("Bearer ", "");
            Map<String, Object> claims = jwtUtil.validateToken(currentToken);
            String managerEmail = claims.get("email").toString();
            log.info("managerEmail: " + managerEmail);

            String deletedUserEmail = managerService.deleteUserByManager(managerEmail, targetUserId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "해당 user의 탈퇴가 완료되었습니다.");
            response.put("deletedUserEmail", deletedUserEmail);

            log.info("delete중인 manager의 email은? : " + deletedUserEmail);

            return ResponseEntity.ok(response);
        } catch (JwtException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "token이 유효하지 않거나 만료됨");

            return ResponseEntity.status(401).body(errorResponse);
        } catch (UserNotFoundException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(404).body(errorResponse);
        } catch (UnauthorizedException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자를 삭제하는 도중 오류가 발생 : " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // product 강제 삭제 메서드
//    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/brand/products/{targetProductId}")
    public ResponseEntity<?> deleteProductByManager(Authentication authentication, @PathVariable Long targetProductId) {
        String managerEmail = authentication.getName();
        log.info("제품 강제 삭제 요청 받음. 관리자: {}, 대상 제품 ID: {}", managerEmail, targetProductId);

        try {
            String deleteProduct = transactionTemplate.execute(status -> {
                try {
                    return managerService.deleteProductByManager(managerEmail, targetProductId);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw e;
                }
            });

            log.info ("제품 삭제 successful. 관리자: {}, 제품 ID: {}", managerEmail, targetProductId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "제품 삭제 완료");
            response.put("deleteProduct", deleteProduct);

            return ResponseEntity.ok(response);
        } catch (ProductNotFoundException e) {
            log.error("제품 not found. 제품 ID: {}", targetProductId);

            Map<String, String> errorReponse = new HashMap<>();
            errorReponse.put("error", "해당하는 제품을 찾을 수 없습니다.");

            return ResponseEntity.status(404).body(errorReponse + e.getMessage());
        } catch (UnauthorizedException e) {
            log.warn("권한 없음. 관리자: {}, 제품 ID: {}", managerEmail, targetProductId);

            Map<String, String> errorReponse = new HashMap<>();
            errorReponse.put("error", "권한이 없습니다.");

            return ResponseEntity.status(403).body(errorReponse + e.getMessage());
        } catch (UserNotFoundException e) {
            log.error("관리자 계정을 찾을 수 없음. 관리자: {}", managerEmail);

            Map<String, String> errorReponse = new HashMap<>();
            errorReponse.put("error", "관리자 계정을 찾을 수 없습니다.");

            return ResponseEntity.status(404).body(errorReponse + e.getMessage());
        } catch (Exception e) {
            log.error("제품 삭제 중 오류가 발생. 관리자: {}, 제품 ID: {}", managerEmail, targetProductId, e);

            Map<String, String> errorReponse = new HashMap<>();
            errorReponse.put("error", "제품 삭제 중 예기치 않은 오류가 발생했습니다.");

            return ResponseEntity.status(500).body(errorReponse + e.getMessage());
        }
    }

    // brand 가입 승인 메서드
    @PostMapping("/brand/approve/{approvalId}")
    public ResponseEntity<?> approvalBrandSignup(Authentication authentication,
                                                 @RequestHeader("Authorization") String token,@PathVariable Long approvalId) {

        Map<String, Object> response = new HashMap<>();

        try {
            String currentToken = token.replace("Bearer ", "");
            Map<String, Object> claims = jwtUtil.validateToken(currentToken);
            String managerEmail = claims.get("email").toString();

            UserSignupDTO approvedBrand = authService.signupBrand(managerEmail, approvalId);

            response.put("status", "success");
            response.put("message", "브랜드 가입이 승인되었습니다.");
            response.put("approvedBrand", approvedBrand);

            return ResponseEntity.ok(response);
        } catch (JwtException e) {
            log.error("JWT 토큰 처리 중 오류 발생", e);
            response.put("status", "error");
            response.put("message", "토큰이 만료됐거나 유효하지 않음");
            return ResponseEntity.status(401).body(response);
        } catch (UserNotFoundException | BrandNotFoundException e) {
            log.warn("승인 대상을 찾을 수 없음: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (UnauthorizedException e) {
            log.warn("권한 없는 작업 시도: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("브랜드 가입 승인 중 오류 발생", e);
            response.put("status", "error");
            response.put("message", "브랜드 가입 승인 중 예기치 않은 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }

    }
}
