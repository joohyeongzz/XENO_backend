
package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.domain.RefreshToken;
import com.daewon.xeno_backend.domain.Users;
import com.daewon.xeno_backend.dto.auth.AuthSigninDTO;
import com.daewon.xeno_backend.dto.auth.AuthSignupDTO;
import com.daewon.xeno_backend.dto.auth.SellerInfoCardDTO;
import com.daewon.xeno_backend.repository.RefreshTokenRepository;
import com.daewon.xeno_backend.security.UsersDetailsService;
import com.daewon.xeno_backend.service.AuthService;
import com.daewon.xeno_backend.utils.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@RequestMapping("/auth")
@RequiredArgsConstructor
@RestController
@EnableWebSecurity
public class AuthController {

    private final JWTUtil jwtUtil;
    private final AuthService authService;
    private final UsersDetailsService usersDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    @GetMapping("/signup")
    public void signupGET() {
        log.info("join get");
    }

    @Operation(summary = "회원가입 처리", description = "회원가입 요청을 처리합니다.")
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid AuthSignupDTO authSignupDTO) {
        log.info("signup post.....");
        log.info(authSignupDTO);

        try {
            Users user = authService.signup(authSignupDTO);
            log.info(user);
            return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
        } catch (AuthService.UserEmailExistException e) {
            log.error("Email already exists: " + authSignupDTO.getEmail(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 이메일입니다.");
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 처리 중 오류가 발생했습니다.");
        }
    }

    @Operation(summary = "판매자 회원가입 처리", description = "판매자 회원가입 요청을 처리합니다.")
    @PostMapping("/signup/seller")
    public Users signupSeller(@RequestBody AuthSignupDTO authSignupDTO) {
        log.info("seller signup post.....");
        log.info(authSignupDTO);

        try {
            Users user = authService.signupSeller(authSignupDTO);
            log.info(user);
            return user;
        } catch (AuthService.UserEmailExistException e) {
            log.error("Email already exists: " + authSignupDTO.getEmail(), e);
        }

        return null;
    }

    @Operation(summary = "로그인 처리", description = "로그인 요청을 처리합니다.")
    @RequestMapping(value = "/signin", method = RequestMethod.POST)
    public ResponseEntity<?> signin(@RequestBody AuthSigninDTO dto) {
        log.info(dto.getEmail());

        // AuthService를 사용하여 사용자의 인증을 시도
        Users users = authService.signin(
                dto.getEmail(),
                dto.getPassword());

        // 사용자가 존재하는 경우
        if (users != null) {
            // 사용자의 이메일을 기반으로 UserDetails를 가져옴
            UserDetails userDetails = usersDetailsService.loadUserByUsername(dto.getEmail());

            // 토큰 생성
            // UsernamePasswordAuthenticationToken, UserId=Principal, Password=Credential 역할을 함
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());

            // SecurityContextHolder에 인증을 설정
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            // JWT Payload에 userId, userName, email, roles 값을 실어서 보냄
            Map<String, Object> claim = new HashMap<>();
            claim.put("userId", users.getUserId());
            claim.put("address", users.getAddress());
            claim.put("phoneNumber", users.getPhoneNumber());
            claim.put("name", users.getName());
            claim.put("email", users.getEmail());
            claim.put("roles", userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));

            String accessToken = jwtUtil.generateToken(claim, 1);
            String refreshToken = jwtUtil.generateToken(claim, 30);

            Optional<RefreshToken> existingRefreshToken = refreshTokenRepository.findByEmail(userDetails.getUsername());

            if (existingRefreshToken.isPresent()) {
                RefreshToken firstRefreshToken = existingRefreshToken.get();
                firstRefreshToken.setToken(refreshToken);
                refreshTokenRepository.save(firstRefreshToken);
            } else {
                RefreshToken newRefreshToken = new RefreshToken();
                newRefreshToken.setToken(refreshToken);
                newRefreshToken.setEmail(userDetails.getUsername());
                refreshTokenRepository.save(newRefreshToken);
            }

            Map<String, String> tokens = Map.of("accessToken", accessToken, "refreshToken", refreshToken);

            // ok()에 클라이언트에게 반환할 토큰을 포함
            // ResponseEntity나 @ResponseBody 어노테이션을 사용하면 스프링은 기본적으로 데이터를 JSON 형식으로 변환하여 클라이언트에게 응답함.
            // 결론은 클라이언트는 JSON 형식으로 데이터를 받게 됨ㅁ
            return ResponseEntity.ok(tokens);
        }else {
            // 401에러 발생
            return ResponseEntity.status(401).body("이메일이나 비밀번호가 맞지 않습니다.");
        }
    }


    @GetMapping("/seller/read")
    public ResponseEntity<?> readSellerInfo(@AuthenticationPrincipal UserDetails userDetails) {

        SellerInfoCardDTO dto = authService.readSellerInfo(userDetails);
        return ResponseEntity.ok(dto);


    }

}

