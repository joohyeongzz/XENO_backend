package com.daewon.xeno_backend.domain.auth;

import com.daewon.xeno_backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BrandApproval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Brand 관련 정보
    @Column(unique = true)
    private String brandName;

    private String companyId;

    // users 관련 정보
    @Column(unique = true)
    private String email;

    private String password;

    private String name;

    private String address;

    private String phoneNumber;

    private String status;

    @ElementCollection(fetch = FetchType.LAZY)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roleSet = new HashSet<>();

    public void addRole(UserRole userRole) {
        this.roleSet.add(userRole);
    }
}
