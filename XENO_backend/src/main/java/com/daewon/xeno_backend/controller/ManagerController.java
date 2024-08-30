package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.domain.auth.Level;
import com.daewon.xeno_backend.domain.auth.UserRole;
import com.daewon.xeno_backend.dto.manager.LevelUpdateDTO;
import com.daewon.xeno_backend.dto.manager.PointUpdateDTO;
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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Log4j2
public class ManagerController {

    private final AuthService authService;
    private final ManagerService managerService;
    private final JWTUtil jwtUtil;
    private final TransactionTemplate transactionTemplate;

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
            log.info("delete중인 manager의 email은? : " + deletedUserEmail);
            return ResponseEntity.ok("해당 user의 탈퇴가 완료되었습니다.");
        } catch (JwtException e) {
            log.error("JWT 토큰 처리 중 오류 발생", e);
            return ResponseEntity.status(401).body("token이 유효하지 않거나 만료됨");
        } catch (UserNotFoundException e) {
            log.warn("사용자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (UnauthorizedException e) {
            log.warn("권한 없는 작업 시도: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            log.error("사용자 삭제 중 오류 발생", e);
            return ResponseEntity.status(500).body("사용자를 삭제하는 도중 오류가 발생 : " + e.getMessage());
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

    // 해당 targetUserId의 point를 조정하는 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/point/{targetUserId}")
    public ResponseEntity<String> updateUserPoint(Authentication authentication,
                                                  @RequestHeader("Authorization") String token,
                                                  @RequestBody PointUpdateDTO pointUpdateDto,
                                                  @PathVariable Long targetUserId) {
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
            return ResponseEntity.ok(String.format("Manager에 의해 user(ID: %d)의 포인트가 %d로 설정되었습니다.", targetUserId, newPoints));
        } catch (JwtException e) {
            log.error("JWT 토큰 처리 중 오류 발생", e);
            return ResponseEntity.status(401).body("토큰이 만료됐거나 유효하지 않음");
        } catch (UserNotFoundException e) {
            log.warn("사용자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (UnauthorizedException e) {
            log.warn("권한 없는 작업 시도: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            log.error("사용자 포인트 업데이트 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body("user point 조정 실패 : " + e.getMessage());
        }
    }

    // 해당 targetUserId의 level를 변경하는 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/level/{targetUserId}")
    public ResponseEntity<String> updateUserLevel(Authentication authentication,
                                                  @RequestHeader("Authorization") String token,
                                                  @RequestBody LevelUpdateDTO levelUpdateDTO,
                                                  @PathVariable Long targetUserId) {
        try {
            String currentToken = token.replace("Bearer ", "");
            Map<String, Object> claims = jwtUtil.validateToken(currentToken);
            String managerEmail = claims.get("email").toString();

            // 새로운 level 값을 String 형태로
            String newLevelString = levelUpdateDTO.getNewLevel();
            // 새로운 level enum 값
            Level newLevel;

            try {
                // enum에 들어있는 값이 대문자로 이루어졌기 때문에 toUpperCase() 사용
                newLevel = Level.valueOf(newLevelString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 Level 값이 입력됨 : {}", newLevelString);
                return ResponseEntity.status(404).body("잘못된 level 값입니다. 허용된 값: " + Arrays.toString(Level.values()));
            }

            managerService.updateUserLevelByManager(managerEmail, targetUserId, newLevel);
            return ResponseEntity.ok("Manager에 의해 user의 level이 정상적으로 변경되었습니다.");
        } catch (JwtException e) {
            log.error("JWT 토큰 처리 중 오류 발생", e);
            return ResponseEntity.status(401).body("토큰이 만료됐거나 유효하지 않음");
        } catch (UserNotFoundException e) {
            log.warn("사용자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (UnauthorizedException e) {
            log.warn("권한 없는 작업 시도: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            log.error("사용자 포인트 업데이트 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body("user level 변경 실패 : " + e.getMessage());
        }
    }

    // brand 강제 탈퇴 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/brand/{targetBrandId}")
    public ResponseEntity<?> deleteBrand(Authentication authentication,
                                         @PathVariable Long targetBrandId) {

        try {
            String deleteBrand = transactionTemplate.execute(new TransactionCallback<String>() {
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
            return ResponseEntity.ok(deleteBrand);
        } catch (BrandNotFoundException e) {
            log.error("브랜드 not found. 브랜드 ID: {}", targetBrandId);
            return ResponseEntity.status(404).body("해당하는 브랜드를 찾을 수 없습니다: " + e.getMessage());
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(403).body("권한이 없습니다: " + e.getMessage());
        }  catch (Exception e) {
            log.error("브랜드 삭제중 예기치 못한 오류 발생. 브랜드 ID: {}", targetBrandId, e);
            return ResponseEntity.status(500).body("브랜드 삭제 중 예기치 않은 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // product 강제 삭제 메서드
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/brand/product/{targetProductId}")
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
            return ResponseEntity.ok(deleteProduct);
        } catch (ProductNotFoundException e) {
            log.error("제품 not found. 제품 ID: {}", targetProductId);
            return ResponseEntity.status(404).body("해당하는 제품을 찾을 수 없습니다: " + e.getMessage());
        } catch (UnauthorizedException e) {
            log.warn("권한 없음. 관리자: {}, 제품 ID: {}", managerEmail, targetProductId);
            return ResponseEntity.status(403).body("권한이 없습니다: " + e.getMessage());
        } catch (UserNotFoundException e) {
            log.error("관리자 계정을 찾을 . 없음. 관리자: {}", managerEmail);
            return ResponseEntity.status(404).body("관리자 계정을 찾을 수 없습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("제품 삭제 중 오류가 발생. 관리자: {}, 제품 ID: {}", managerEmail, targetProductId, e);
            return ResponseEntity.status(500).body("제품 삭제 중 예기치 않은 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
