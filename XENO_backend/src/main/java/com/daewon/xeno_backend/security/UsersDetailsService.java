
package com.daewon.xeno_backend.security;

import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.auth.AuthSigninDTO;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Spring Security에서 사용자 인증을 처리하기 위해 UserDetailsService 인터페이스를 구현한 서비스 클래스
@Service
@Log4j2
@RequiredArgsConstructor
public class UsersDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // 이메일을 기반으로 사용자 정보를 로드하는 메서드
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("loadUserByEmail : " + email);

        // 이메일을 사용하여 사용자 조회
        Optional<Users> result = userRepository.findByEmail(email);
        log.info("result : " + result);

        if (!result.isPresent()) {
            throw new UsernameNotFoundException("Cannot find user with Email : " + email);
        }

        Users users = result.get(); // Optional에서 사용자 객체를 추출

        log.info("User found: " + users);

        log.info("UsersDetailsService----------------------------------------------");

        // ROLE 역할이 비어있는지 확인
        if (users.getRoleSet().isEmpty()) {
            log.error("User has no roles assigned");
            throw new UsernameNotFoundException("User has no roles assigned");
        }

        // 권한을 SimpleGrantedAuthority로 변환
        // 사용자의 역할을 SimpleGrantedAuthority로 변환하여 Spring Security의 권한 시스템과 통합
        List<SimpleGrantedAuthority> authorities;
        try {
            authorities = users.getRoleSet().stream()
                    .peek(role -> log.info("유저한테 할당된 권한: ROLE_" +role))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error converting roles to authorities", e);
            throw new UsernameNotFoundException("Cannot convert roles to authorities", e);
        }

        // AuthSigninDTO 객체 생성 및 반환
        return new AuthSigninDTO(
                users.getUserId(),
                users.getEmail(),
                users.getPassword(),
                users.getName(),
                authorities
        );
    }
}

