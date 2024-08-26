package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.auth.Brand;
import com.daewon.xeno_backend.domain.auth.UserRole;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.auth.SellerDTO;
import com.daewon.xeno_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
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
                .companyId(dto.getCompanyId())
                .build();
        brand.addRole(UserRole.SELLER);

        // Users에 Brand 설정
        user.setBrand(brand);

        // User 저장 (Brand도 함께 저장됨)
        return userRepository.save(user);
    }
}
