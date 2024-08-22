package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.auth.Level;
import com.daewon.xeno_backend.domain.auth.UserRole;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.auth.AuthSignupDTO;
import com.daewon.xeno_backend.dto.auth.SellerInfoCardDTO;
import com.daewon.xeno_backend.dto.auth.TokenDTO;
import com.daewon.xeno_backend.repository.RefreshTokenRepository;
import com.daewon.xeno_backend.repository.UserRepository;
import com.daewon.xeno_backend.utils.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;

    @Override
    public Users signup(AuthSignupDTO authSignupDTO) throws UserEmailExistException {
        if(userRepository.existsByEmail(authSignupDTO.getEmail())) {
            throw new UserEmailExistException();
        }

        Users users = modelMapper.map(authSignupDTO, Users.class);

        users.setPassword(passwordEncoder.encode(authSignupDTO.getPassword()));
        users.addRole(UserRole.USER);
        users.addLevel(Level.BRONZE);

        log.info("================================");
        log.info(users);
        log.info(users.getRoleSet());

        userRepository.save(users);
        return users;
    }
    
    // 판매자 회원가입 추가
    @Override
    public Users signupSeller(AuthSignupDTO authSignupDTO) throws UserEmailExistException {
        if(userRepository.existsByEmail(authSignupDTO.getEmail())) {
            throw new UserEmailExistException();
        }
    
        Users users = modelMapper.map(authSignupDTO, Users.class);

        users.setName(authSignupDTO.getName());  // 명시적으로 이름 설정
        users.setPassword(passwordEncoder.encode(authSignupDTO.getPassword()));
        users.addRole(UserRole.SELLER);
//        users.setCompanyId(authSignupDTO.getCompanyId());
//        users.setBrandName(authSignupDTO.getBrandName());
        users.setAddress(authSignupDTO.getAddress());
        users.setPhoneNumber(authSignupDTO.getPhoneNumber());
    
        log.info("================================");
        log.info(users);
        log.info(users.getRoleSet());
    
        userRepository.save(users);
        return users;
    }

    @Override
    public Users signin(String email, String password) {
        // log.info("클라이언트 이메일 : " + email);

        Optional<Users> optionalUsers = userRepository.findByEmail(email);
        // 클라이언트한테 받은 password값과 db에 있는 password값 대조
        // DB에 저장되어 있는 password값이 인코딩 되어서 저장되어 있으므로
        // 클라이언트한테 받은 password값을 인코딩 시켜 대조 시켜야 함
        // 앞에 password는 클라이언트한테 받은 패스워드, 뒤에는 db에 있는 패스워드 값
        if (optionalUsers.isPresent()) {
            if (passwordEncoder.matches(password, optionalUsers.get().getPassword()) && email.equals(optionalUsers.get().getEmail())) {
                log.info("클라이언트 이메일 :" + email);
                log.info("클라이언트 패스워드 : " + password);
                log.info("login success");
                return optionalUsers.get();
            } else {
                log.info("클라이언트 패스워드 : " + password);
                log.info("login fail");
                log.info("클라이언트가 입력한 패스워드" + password +"을 DB에서 찾지 못했습니다.");
                return null;
            }
        } else {
            log.info("클라이언트 이메일 :" + email);
            log.info("login fail");
            log.info("클라이언트가 입력한 이메일" + email + "을 DB에서 찾지 못했습니다");
            return null;
        }
    }

    @Override
    public SellerInfoCardDTO readSellerInfo(UserDetails userDetails) {
        Users users = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        SellerInfoCardDTO dto = new SellerInfoCardDTO();
//                dto.setBrandName(users.getBrandName());
                dto.setName(users.getName());

        return dto;
    }

    @Override
    public TokenDTO tokenReissue(String refreshToken) {

        Map<String, Object> claims = jwtUtil.validateToken(refreshToken);

        String email = (String) claims.get("email");

        String newAccessToken = jwtUtil.generateToken(Map.of("email", email), 1); // 30분 유효

        return new TokenDTO(newAccessToken, refreshToken);
    }
}
