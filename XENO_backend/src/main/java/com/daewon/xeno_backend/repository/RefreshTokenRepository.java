package com.daewon.xeno_backend.repository;

import com.daewon.xeno_backend.domain.RefreshToken;
import com.daewon.xeno_backend.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    void deleteByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.token = :newToken WHERE rt.email = :username")
    int updateTokenByUsername(@Param("newToken") String newToken, @Param("username") String username);

    Optional<RefreshToken> findByEmail(String email);
}