package com.daewon.xeno_backend.domain.auth;

import com.daewon.xeno_backend.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long brandId;

    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    @JsonBackReference
    private Set<Users> user = new HashSet<>();

    @Column(unique = true)
    private String brandName;

    private String companyId;

    @ElementCollection(fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRole> roleSet = new HashSet<>();

    public void addRole(UserRole userRole) {
        this.roleSet.add(userRole);
    }

    public void addUser(Users user) {
        if (this.user == null) {
            this.user = new HashSet<>();
        }
        this.user.add(user);
        user.setBrand(this);
    }
}
