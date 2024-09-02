package com.daewon.xeno_backend.dto.manager;

import com.daewon.xeno_backend.domain.auth.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class BrandListDTO {
    private Long brandId;
    private String brandName;
    private String companyId;
    private Set<UserRole> roles;
    private List<UserInfoDTO> users;
}
