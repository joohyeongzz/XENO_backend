package com.daewon.xeno_backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {

    private String password;
    private String name;
    private String address;
    private String phoneNumber;
}
