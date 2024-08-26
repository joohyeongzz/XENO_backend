package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.auth.Brand;
import com.daewon.xeno_backend.domain.auth.UserRole;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.auth.BrandDTO;
import com.daewon.xeno_backend.dto.signup.BrandRegisterDTO;
import com.daewon.xeno_backend.dto.signup.UserRegisterDTO;
import com.daewon.xeno_backend.repository.BrandRepository;
import com.daewon.xeno_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService2 {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrandRepository brandRepository;

    public UserRegisterDTO registerSellerUser(BrandDTO dto) {
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

    private UserRegisterDTO convertToDTO(Users user) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setAddress(user.getAddress());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRoleSet(user.getRoleSet().stream().map(Enum::name).collect(Collectors.toSet()));

        if (user.getBrand() != null) {
            BrandRegisterDTO brandDTO = new BrandRegisterDTO();
            brandDTO.setBrandId(user.getBrand().getBrandId());
            brandDTO.setBrandName(user.getBrand().getBrandName());
            brandDTO.setCompanyId(user.getBrand().getCompanyId());
            brandDTO.setRoleSet(user.getBrand().getRoleSet().stream().map(Enum::name).collect(Collectors.toSet()));
            dto.setBrand(brandDTO);
        }

        return dto;
    }
}
