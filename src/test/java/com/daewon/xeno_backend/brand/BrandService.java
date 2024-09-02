package com.daewon.xeno_backend.brand;

import com.daewon.xeno_backend.domain.auth.Brand;
import com.daewon.xeno_backend.domain.auth.UserRole;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@SpringBootTest
@Service
@Log4j2
@RequiredArgsConstructor
public class BrandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Users registerSellerUser(SellerDTO dto) {
        // Users 객체 생성
        Users user = Users.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .address(dto.getAddress())
                .phoneNumber(dto.getPhoneNumber())
                .build();
        user.addRole(UserRole.SELLER);

        // Brand 객체 생성
        Brand brand = Brand.builder()
                .brandName(dto.getBrandName())
                .companyId(Long.parseLong(dto.getCompanyId()))
                .build();
        brand.addRole(UserRole.SELLER);

        // Users에 Brand 설정
        user.setBrand(brand);

        // User 저장 (Brand도 함께 저장됨)
        return userRepository.save(user);
    }

}
