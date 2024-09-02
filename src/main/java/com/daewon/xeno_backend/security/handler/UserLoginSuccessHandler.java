
package com.daewon.xeno_backend.security.handler;

import com.daewon.xeno_backend.dto.auth.AuthSigninDTO;
import com.daewon.xeno_backend.utils.JWTUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// 인증 성공 후 처리 작업을 담당
@Log4j2
@RequiredArgsConstructor
public class UserLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("Login Success Handler.............");

        // 응답 콘텐츠 타입을 JSON으로 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        log.info(authentication.toString());
        log.info("Username: " + authentication.getName()); // username


        String email = ((AuthSigninDTO) authentication.getPrincipal()).getEmail();
        log.info("Email: " + email);
        AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
        Long userId = authSigninDTO.getUserId();
        log.info("userId: " + userId);

        // 클레임 생성 : JWT 토큰에 포함될 클레임(사용자 정보)을 생성
        // JWTUtil payload부분에서 .putall로 userid, email, roles 값이 들어감
        Map<String, Object> claim = new HashMap<>();
        claim.put("userId", userId);
        claim.put("email", email);
        claim.put("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        // Access Token 유효기간 1일
        String accessToken = jwtUtil.generateToken(claim,1);
        // Refresh Token 유효기간 30일
        String refreshToken = jwtUtil.generateToken(claim,30);

        // successToken, refreshToken이 포함된 JSON 응답 생성
        Gson gson = new Gson();
        Map<String, String> keyMap = Map.of("accessToken", accessToken, "refreshToken", refreshToken);

        String jsonStr = gson.toJson(keyMap);

        // JSON 응답을 출력에 작성
        response.getWriter().println(jsonStr);
    }
}

