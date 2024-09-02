package com.daewon.xeno_backend.dto.manager;

import com.daewon.xeno_backend.domain.auth.Level;
import com.daewon.xeno_backend.domain.auth.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserListDTO {
    private Long userId;
    private String email;
    private String name;
    private String phoneNumber;
    private String address;
    private Set<UserRole> roles;
    private Long customerId;
    private int point;
    private Level level;
}
