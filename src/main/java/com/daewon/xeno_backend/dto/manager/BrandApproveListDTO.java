package com.daewon.xeno_backend.dto.manager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandApproveListDTO {

    private Long id;
    private String brandName;
    private String companyId;
    private String email;
    private String name;
    private String phoneNumber;
    private String address;
    private String status;
}
