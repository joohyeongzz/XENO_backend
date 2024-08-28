package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.auth.*;
import com.daewon.xeno_backend.dto.auth.*;
import com.daewon.xeno_backend.dto.user.UserUpdateDTO;
import com.daewon.xeno_backend.exception.UserNotFoundException;
import com.daewon.xeno_backend.repository.RefreshTokenRepository;
import com.daewon.xeno_backend.repository.auth.BrandRepository;
import com.daewon.xeno_backend.repository.auth.CustomerRepository;
import com.daewon.xeno_backend.repository.auth.ManagerRepository;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import com.daewon.xeno_backend.utils.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final BrandRepository brandRepository;
    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public Users signup(UserSignupDTO userSignupDTO) throws UserEmailExistException {
        if(userRepository.existsByEmail(userSignupDTO.getEmail())) {
            throw new UserEmailExistException();
        }

        // Customer 엔티티를 먼저 생성.
        Customer customer = Customer.builder()
                .point(1000)
                .level(Level.BRONZE)
                .build();

        // Users 엔티티를 생성하고 저장.
        Users user = Users.builder()
                .email(userSignupDTO.getEmail())
                .password(passwordEncoder.encode(userSignupDTO.getPassword()))
                .name(userSignupDTO.getName())
                .address(userSignupDTO.getAddress())
                .phoneNumber(userSignupDTO.getPhoneNumber())
                .customer(customer)
                .build();

        user.addRole(UserRole.USER);
        user = userRepository.save(user);

        // Customer에 Users엔티티를 생성할때 할당받은 userId를 설정하고 저장.
        customer.setUserId(user.getUserId());
        customerRepository.save(customer);

        return user;
    }

    // 판매사 회원가입
    @Override
    public UserSignupDTO signupBrand(BrandDTO dto) {
        Brand brand = brandRepository.findByBrandName(dto.getBrandName())
                .orElseGet(() -> {
                    if (dto.getCompanyId() == null) {
                        throw new IllegalArgumentException("New brand requires a company ID");
                    }
                    Brand newBrand = Brand.builder()
                            .brandName(dto.getBrandName())
                            .companyId(dto.getCompanyId())
                            .build();
                    newBrand.addRole(UserRole.SELLER);
                    return brandRepository.save(newBrand);
                });

        Users user = Users.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .address(dto.getAddress())
                .phoneNumber(dto.getPhoneNumber())
                .brand(brand)
                .build();
        user.addRole(UserRole.SELLER);

        Users savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    // 관리자 회원가입
    @Override
    public Manager signupManager(UserSignupDTO userSignupDTO) {
        Manager manager = Manager.builder()
                .build();

        Users user = Users.builder()
                .email(userSignupDTO.getEmail())
                .password(passwordEncoder.encode(userSignupDTO.getPassword()))
                .name(userSignupDTO.getName())
                .address(userSignupDTO.getAddress())
                .phoneNumber(userSignupDTO.getPhoneNumber())
                .manager(manager)
                .build();

        user.addRole(UserRole.MANAGER);
        user = userRepository.save(user);

        // Customer에 Users엔티티를 생성할때 할당받은 userId를 설정하고 저장.
        manager.setUserId(user.getUserId());
        managerRepository.save(manager);

        return manager;
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

    // user정보 password, name, address, phoneNumber 수정
    @Transactional
    @Override
    public Users updateUser(String email, UserUpdateDTO updateDTO) throws UserNotFoundException {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User email을 찾을 수 없음 : " + email));

        // password 업데이트
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updateDTO.getPassword()));
        }

        // name 업데이트
        if (updateDTO.getName() != null && !updateDTO.getName().isEmpty()) {
            user.setName(updateDTO.getName());
        }

        // address 업데이트
        if (updateDTO.getAddress() != null && !updateDTO.getAddress().isEmpty()) {
            user.setAddress(updateDTO.getAddress());
        }

        // phoneNumber 업데이트
        if (updateDTO.getPhoneNumber() != null && !updateDTO.getPhoneNumber().isEmpty()) {
            user.setPhoneNumber(updateDTO.getPhoneNumber());
        }

        return userRepository.save(user);
    }

    @Transactional
    @Override
    public void deleteUser(String email) throws UserNotFoundException {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User의 email을 찾을수 없습니다 : " + email));

        // Customer 엔티티 삭제
        if (user.getCustomer() != null) {
            customerRepository.delete(user.getCustomer());
        }

        // Users 엔티티 삭제
        userRepository.delete(user);
    }

//    @Transactional
//    public void deleteUser(String userEmail) throws UserNotFoundException {
//        log.info("Attempting to delete user with email: {}", userEmail);
//
//        Users user = userRepository.findByEmail(userEmail)
//                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));
//
//        log.info("User found. User ID: {}", user.getUserId());
//
//        // Customer 엔티티 삭제
//        try {
//            if (user.getCustomer() != null) {
//                log.info("Deleting customer for user: {}", userEmail);
//                customerRepository.delete(user.getCustomer());
//            }
//        } catch (Exception e) {
//            log.error("Error deleting customer for user: {}", userEmail, e);
//            throw new RuntimeException("Failed to delete customer", e);
//        }
//
//        // RefreshToken 삭제
//        try {
//            log.info("Deleting refresh tokens for user: {}", userEmail);
//            refreshTokenRepository.deleteByEmail(userEmail);
//        } catch (Exception e) {
//            log.error("Error deleting refresh tokens for user: {}", userEmail, e);
//            throw new RuntimeException("Failed to delete refresh tokens", e);
//        }
//
//        // Users 엔티티 삭제
//        try {
//            log.info("Deleting user: {}", userEmail);
//            userRepository.delete(user);
//        } catch (Exception e) {
//            log.error("Error deleting user: {}", userEmail, e);
//            throw new RuntimeException("Failed to delete user", e);
//        }
//
//        log.info("User successfully deleted: {}", userEmail);
//    }

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

    private UserSignupDTO convertToDTO(Users user) {
        UserSignupDTO dto = new UserSignupDTO();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setAddress(user.getAddress());
        dto.setPhoneNumber(user.getPhoneNumber());

        if (user.getBrand() != null) {
            BrandSignupDTO brandDTO = new BrandSignupDTO();
            brandDTO.setBrandId(user.getBrand().getBrandId());
            brandDTO.setBrandName(user.getBrand().getBrandName());
            brandDTO.setCompanyId(user.getBrand().getCompanyId());
        }

        return dto;
    }
}
