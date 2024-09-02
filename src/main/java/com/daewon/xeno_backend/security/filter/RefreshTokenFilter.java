
package com.daewon.xeno_backend.security.filter;

import com.daewon.xeno_backend.domain.RefreshToken;
import com.daewon.xeno_backend.repository.RefreshTokenRepository;
import com.daewon.xeno_backend.security.exception.RefreshTokenException;
import com.daewon.xeno_backend.utils.JWTUtil;
import com.google.gson.Gson;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor
public class RefreshTokenFilter extends OncePerRequestFilter {

    private final String refreshToken;
    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("path: " + path);

        // url주소에 /api이 없으면 refresh Token filter를 skip
        if(!path.contains("/refreshToken")) {
            log.info("skip refresh token filter..........");
            filterChain.doFilter(request, response);
            return;
        }

        log.info("Refresh token filter..........run......");

        // 전송된 JSON에서 accessToken과 refreshToken을 얻어온다
        Map<String, String> tokens = parseRequstJSON(request);
        log.info("tokens: " + tokens);

        if (tokens == null || tokens.isEmpty()) {
            log.warn("토큰 정보를 파싱할 수 없습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        log.info("accessToken : " + accessToken);
        log.info("refreshToken : " + refreshToken);

        if (accessToken == null || refreshToken == null) {
            log.warn("accessToken 또는 refreshToken이 제공되지 않았습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        // accessToken 관련 exception
        try {
            checkAccessToken(accessToken);
        } catch (RefreshTokenException refreshTokenException) {
            refreshTokenException.sendResponseError(response);
            return;
        }

        Map<String, Object> refreshClaims = null;
        // refreshToken 관련 exception

        try {
            refreshClaims = checkRefreshToken(refreshToken);
            log.info(refreshClaims);

            // 새로운 Access Token 발행
            // Refrsh Token은 만료일이 얼마 남지 않은 경우 새로 발행

            // Refresh Token의 유효 시간이 얼마 남지 않은 경우
            Long exp = (Long) refreshClaims.get("exp");
            Date expTime = new Date(Instant.ofEpochMilli(exp).toEpochMilli() * 1000);   // 만료 시간
            Date current = new Date(System.currentTimeMillis());                        // 현재 시간

            // 만료 시간과 현재 시간의 간격 계산
            // 만일 3일 미만인 경우에는 Refresh Token도 다시 생성
            long gapTime = (expTime.getTime() - current.getTime());     // 토큰 유효기간 남은 시간

            log.info("------------------------------");
            log.info("현재 시간 : " + current);
            log.info("만료 시간 : " + expTime);
            log.info("토큰 유효기간 남은 시간 : " + gapTime);

            String email = (String) refreshClaims.get("email");

            // 이 상태까지 오면 무조건 AccessToken은 새로 생성
            String accessTokenValue = jwtUtil.generateToken(Map.of("email", email), 1);
            String refreshTokenValue = tokens.get("refreshToken");

            // RefreshToken이 3일도 안 남았다면
            // 1000 * 60 * 60 * 24 * 3
            if(gapTime < (1000* 60 * 60 * 24 * 3)) {
                log.info("새로운 Refresh Token 발급");
                refreshTokenValue = jwtUtil.generateToken(Map.of("email", email), 30);
            }

            log.info("Refresh Token result.......................");
            log.info("accessToken: " + accessTokenValue);
            log.info("refreshToken: " + refreshTokenValue);

            // 새로운 토큰들을 생성한 후 sendTokens()를 호출
            sendTokens(accessTokenValue, refreshTokenValue, response);

        } catch (RefreshTokenException refreshTokenException) {
            refreshTokenException.sendResponseError(response);
            return;
        }
    }

    private void updateRefreshTokenInDB(String email, String newRefreshToken) {
        Optional<RefreshToken> userEmail = refreshTokenRepository.findByEmail(email);
        if(userEmail.isPresent()) {
            RefreshToken refreshToken = userEmail.get();
            refreshToken.setToken(newRefreshToken);
            refreshTokenRepository.save(refreshToken);
        } else {
            log.error("해당하는 이메일에 refreshToken을 찾을수 없음: " + email);
        }
    }

//    private Map<String, String> parseRequstJSON(HttpServletRequest request) {
//
//        // JSON 데이터를 분석해서 email, password 전달 값을 Map으로 처리
//        try(Reader reader = new InputStreamReader(request.getInputStream())) {
//            Gson gson = new Gson();
//            log.info("gson??: " + gson.toJson(reader));
//
//            return gson.fromJson(reader, Map.class);
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
//        return null;
//    }

    private Map<String, String> parseRequstJSON(HttpServletRequest request) {
        Map<String, String> tokenMap = new HashMap<>();

        try {
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                StringBuilder stringBuilder = new StringBuilder();
                BufferedReader bufferedReader = request.getReader();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String jsonBody = stringBuilder.toString();

                Gson gson = new Gson();
                Map<String, String> parsedMap = gson.fromJson(jsonBody, Map.class);

                if (parsedMap != null) {
                    tokenMap.putAll(parsedMap);
                }
            } else {
                // JSON이 아닌 경우, 파라미터에서 토큰 추출
                tokenMap.put("accessToken", request.getParameter("accessToken"));
                tokenMap.put("refreshToken", request.getParameter("refreshToken"));
            }
        } catch (Exception e) {
            log.error("토큰 파싱 중 오류 발생: " + e.getMessage(), e);
        }

        return tokenMap;
    }

    // accessToken 검증
    private void checkAccessToken(String accessToken) throws RefreshTokenException {
        try {
            jwtUtil.validateToken(accessToken);
        } catch (ExpiredJwtException expiredJwtException) { // 만료 기간이 지났을 떄
            log.info("Access Token has expired");
        } catch (Exception exception) {     // 나머지 상황
            throw new RefreshTokenException(RefreshTokenException.ErrorCase.NO_ACCESS);
        }
    }

    // refreshToken 검증
    // refreshToken이 존재하는지와 만료이 지났는지 확인, 새로운 토큰 생성을 위해 email 값을 얻어 둠
    private Map<String, Object> checkRefreshToken(String refreshToken) throws RefreshTokenException {
        try {
            Map<String, Object> values = jwtUtil.validateToken(refreshToken);
            return values;
        } catch (ExpiredJwtException expiredJwtException) {
            throw new RefreshTokenException(RefreshTokenException.ErrorCase.OLD_REFRESH);
        } catch (MalformedJwtException malformedJwtException) {
            log.error("MalformedJwtException-----------------------");
            throw new RefreshTokenException(RefreshTokenException.ErrorCase.NO_REFRESH);
        } catch (Exception exception) {
            new RefreshTokenException(RefreshTokenException.ErrorCase.NO_REFRESH);
        }
        return null;
    }

    // 만들어진 토큰들을 전송하는 sendTokens()
    private void sendTokens(String accessTokenValue, String refreshTokenValue, HttpServletResponse response) {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Gson gson = new Gson();
        String jsonStr = gson.toJson(Map.of("accessToken", accessTokenValue, "refreshToken", refreshTokenValue));

        try {
            response.getWriter().println(jsonStr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

