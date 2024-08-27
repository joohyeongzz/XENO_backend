package com.daewon.xeno_backend.dto.auth;

import com.daewon.xeno_backend.domain.auth.Customer;
import com.daewon.xeno_backend.dto.signup.BrandRegisterDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSignupDTO {

    private Long userId;
    private String email;
    private String password;
    private String name;
    private String address;
    private String phoneNumber;
    private BrandRegisterDTO brand;
    private Customer customer;
}
