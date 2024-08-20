
package com.daewon.xeno_backend.dto.auth;

import lombok.Data;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;


@ToString
@Data
public class AuthSigninDTO implements UserDetails {

    private Long userId;

    private String email;

    private String password;

    private String name;

    private Collection<? extends GrantedAuthority> authorities;


    public AuthSigninDTO(Long userId, String email, String password,
                         String name, Collection<? extends GrantedAuthority> authorities) {

        this.userId = userId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.authorities = authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // 또는 userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}

