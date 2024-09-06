package com.daewon.xeno_backend.dto.auth;

import com.daewon.xeno_backend.domain.auth.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSignupDTO {

    private Long userId;
    private String email;
    private String password;
    private String name;
    private String address;
    private String phoneNumber;
    private BrandSignupDTO brand;
    private Customer customer;
}
