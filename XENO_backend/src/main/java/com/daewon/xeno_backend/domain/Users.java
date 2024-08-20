package com.daewon.xeno_backend.domain;


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
public class Users extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment와 같은
  private Long userId;

  private String password;

  @Column(unique = true)
  private String email;

  private String brandName;

  private String name;

  private String address;

  private String phoneNumber;

  private String companyId;

  @ElementCollection(fetch = FetchType.LAZY)
  @Builder.Default
  private Set<UserRole> roleSet = new HashSet<>();

  public void addRole(UserRole userRole) {
    this.roleSet.add(userRole);
  }

}
