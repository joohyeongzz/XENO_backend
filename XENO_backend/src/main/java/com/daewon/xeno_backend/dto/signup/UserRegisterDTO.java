package com.daewon.xeno_backend.dto.signup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterDTO {

    private Long userId;
    private String email;
    private String name;
    private String address;
    private String phoneNumber;
    private BrandRegisterDTO brand;
    private Set<String> roleSet;
}
