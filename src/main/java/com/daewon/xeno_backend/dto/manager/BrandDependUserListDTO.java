package com.daewon.xeno_backend.dto.manager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandDependUserListDTO {
    private Long userId;
    private String email;
    private String name;
    private Long brandId;
}
