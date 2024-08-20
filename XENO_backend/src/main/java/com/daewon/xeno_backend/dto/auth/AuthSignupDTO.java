package com.daewon.xeno_backend.dto.auth;

import lombok.Data;

import java.util.Date;

@Data
public class AuthSignupDTO {
    private String password;
    private String name;
    private String email;
    private String companyId;
    private boolean isSeller;
    private String address;
    private String phoneNumber;
    private String brandName;

    private Date createAt;
}
