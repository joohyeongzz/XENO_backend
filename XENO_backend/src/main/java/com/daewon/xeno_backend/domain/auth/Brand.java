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
public class Brand extends BaseEntity {

    @Id
    private Long brandId;

    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Users> user = new HashSet<>();

    private String brandName;

    private Long companyId;

    @ElementCollection(fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRole> roleSet = new HashSet<>();

    public void addRole(UserRole userRole) {
        this.roleSet.add(userRole);
    }
}
