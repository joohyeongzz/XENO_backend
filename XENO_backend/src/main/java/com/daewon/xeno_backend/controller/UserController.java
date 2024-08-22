package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.dto.auth.TokenDTO;
import com.daewon.xeno_backend.dto.reply.ReplyReadDTO;
import com.daewon.xeno_backend.exception.UserNotFoundException;
import com.daewon.xeno_backend.repository.RefreshTokenRepository;
import com.daewon.xeno_backend.service.AuthService;
import com.daewon.xeno_backend.service.ReplyService;
import com.daewon.xeno_backend.utils.JWTUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;
    private final ReplyService replyService;
    private final AuthService authService;

    // 클라이언트가 accessToken을 서버에 보내면
    // 서버에서 해당 토큰을 검증하고 payload 전체를 JSON 형식으로 클라이언트에게 반환이 가능
    // 테스트할때 Authorization에 Bearer (accessToken Value) - 이렇게 보내면 됨
    @GetMapping()
    public ResponseEntity<?> readUser(@RequestHeader("Authorization") String token) {
        try {
            // Bearer 토큰에서 "Bearer " 부분 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰에서 payload 추출
            Map<String, Object> claims = jwtUtil.validateToken(token);

            log.info("payload값 서버 -> 클라이언트 : " + claims);
            // payload 반환
            return ResponseEntity.ok(claims);
        } catch (JwtException e) {
            // 토큰이 유효하지 않으면 401 상태 코드 반환
            return ResponseEntity.status(401).body("토큰이 유효하지 않습니다.");
        }
    }

    @GetMapping("/token")
    public ResponseEntity<?> readToken(@RequestHeader("Authorization") String token) {
        try {
            // Bearer 토큰에서 "Bearer " 부분 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // payload 반환
            Map<String, Object> Message = new HashMap<>();

            Message.put("message", "유효한 토큰입니다.");

            return ResponseEntity.ok(Message);

        } catch (UserNotFoundException e) {
            return ResponseEntity.status(403).body("유저 정보가 맞지 않습니다.");
        } catch (JwtException e) {
            return ResponseEntity.status(401).body("토큰이 유효하지 않습니다.");
        }
    }

    // refreshToken으로 accessToken을 재발급하는 메서드, accessToken과 refreshToken값을 같이 JSON 값으로 넘겨줘야함.
    @PostMapping("/refreshToken")
    public ResponseEntity<?> refreshToken(@RequestBody TokenDTO tokenDTO) {
        log.info("RefreshToken 토큰 요청을 합니다.");

        String refreshToken = tokenDTO.getRefreshToken();

        try {
            // refreshToken을 검증하고 새로운 accessToken을 발급
            TokenDTO newTokenDto = authService.tokenReissue(refreshToken);

            log.info("새로운 access token 발급 생성 완료");
            return ResponseEntity.ok(newTokenDto);
        } catch (Exception e) {
            log.error("refresh token이 맞지 않음: " + e.getMessage());
            return ResponseEntity.status(403).body("refresh token이 맞지 않음");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        log.info("logout token(successToken) : " + token);
        // "Bearer " 부분 제거 - 토큰 값이 "Bearer ${accessToken}" 이 방식으로 들어가기 때문
        String refreshToken = token.substring(7);
        log.info("refreshToken : " + refreshToken);
        // Refresh Token 삭제
        refreshTokenRepository.deleteByToken(refreshToken);
        // SecurityContextHolder 초기화
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("Logout successful");
    }

    // 특정 사용자가 작성한 Reply 목록 조회
    // 로그인한 사용자의 Reply를 가져오는 엔드포인트
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/reply")
    public ResponseEntity<?> getUserReplys(@RequestHeader("Authorization") String token) {
        try {
            // Bearer 토큰에서 "Bearer " 부분 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰에서 payload 추출
            Map<String, Object> claims = jwtUtil.validateToken(token);
            // payload에서 userId 추출
            Long userId = Long.parseLong(claims.get("userId").toString());

            log.info("유저 ID: " + userId);

            // 해당 사용자가 작성한 Reply 가져오기
            List<ReplyReadDTO> replies = replyService.getRepliesByUserId(userId);

            // 댓글 목록 반환
            return ResponseEntity.ok(replies);
        } catch (JwtException e) {
            // 토큰이 유효하지 않으면 401 상태 코드 반환
            return ResponseEntity.status(401).body("Invalid token");
        }
    }
}
