package com.daewon.xeno_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrandSignupDTO {
    private Long brandId;
    private String brandName;
    private String companyId;
    private Set<String> roleSet;
}
