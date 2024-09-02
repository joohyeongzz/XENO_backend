
package com.daewon.xeno_backend.security.filter;

import com.daewon.xeno_backend.security.UsersDetailsService;
import com.daewon.xeno_backend.security.exception.AccessTokenException;
import com.daewon.xeno_backend.utils.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

// JWT값 검증 필터
// JWTUtil의 validateToken() 기능 활용
@Log4j2
@RequiredArgsConstructor
public class TokenCheckFilter extends OncePerRequestFilter {    // OncePerRequestFilter는 하나의 요청에[ 대해서 한번씩 동작하는 필터

    private final JWTUtil jwtUtil;  // 의존성 주입으로 jwtUtil의 validateToken을 불러와야함
    // userDetailsService를 이용해서 JWT의 email 값으로 사용자 정보를 얻어옴
    private final UsersDetailsService usersDetailsServicea;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if(!path.startsWith("/api")) {  // /api로 시작하지 않는 경우는 리턴
            filterChain.doFilter(request,response); // 다음 필터로 넘김
            return;
        }

        log.info("Token Check Filter......");
        log.info("JWTUtil : " + jwtUtil);

        // 경로가 /api로 시작하는 경우 validateAccessToken 메서드를 호출하여 JWT 토큰을 검증함
        try {
            Map<String, Object> payload = validateAccessToken(request);

            // email
            String email = (String) payload.get("email");
            log.info("email : " + email);

            UserDetails userDetails = usersDetailsServicea.loadUserByUsername(email);

            // 등록 사용자 인증 정보 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, userDetails.getPassword(), userDetails.getAuthorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (AccessTokenException accessTokenException) {
            accessTokenException.sendResponseError(response);
            log.error("accessToken error" + accessTokenException);
        }
    }

    // 클라이언트에서 보낸 Access Token 검증
    private Map<String, Object> validateAccessToken(HttpServletRequest request) throws AccessTokenException {

        // 클라이언트가 서버로 요청을 보낼 때 HTTP 요청 헤더에 포함된 AccessToken을 추출
        // "Authorization" 헤더를 사용하여 토큰을 전달한 토큰을 받아옴
        // Authorization: Bearer ${access_token} - 클라이언트에서 이 값으로 보내줘야 함
        String headerStr = request.getHeader("Authorization");
        log.info("headerStr의 length : " + headerStr.length());

        // Authorization 헤더가 없거나 길이가 8미만이면 UNACCEPT 예외를 던짐
        // 8미만으로 짜르는 이유는 headerStr이 "Bearer <access_token>" 이렇게 이루어져 있기 때문
        if(headerStr == null || headerStr.length() < 8) {
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.UNACCEPT);
        }

        // Bearer 생략
        String tokenType = headerStr.substring(0, 6);
        String tokenStr = headerStr.substring(7);

        // 타입이 Bearer인지 확인하고 올바르지 않으면 BADTYPE 예외를 던짐
        if(tokenType.equalsIgnoreCase("Bearer") == false) {
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.BADTYPE);
        }

        try {
            Map<String, Object> values = jwtUtil.validateToken(tokenStr);

            return values;
        } catch (MalformedJwtException malformedJwtException) {
            log.info("MalformedJwtException-----------------------");
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.MALFORM);
        } catch (SignatureException signatureException) {
            log.info("SignatureException---------------------------");
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.BADSIGN);
        } catch (ExpiredJwtException expiredJwtException) {
            log.info("ExpiredJwtException--------------------------");
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.EXPIRED);
        }
    }
}

