package com.daewon.xeno_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellerDTO {

    private String email;
    private String password;
    private String name;
    private String address;
    private String phoneNumber;
    private String brandName;
    private String companyId;
}
