package com.daewon.xeno_backend.domain.auth;


import com.daewon.xeno_backend.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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

  private String name;

  private String address;

  private String phoneNumber;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "brandId", referencedColumnName = "brandId")
  private Brand brand;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "managerId", referencedColumnName = "managerId")
  private Manager manager;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "customerId", referencedColumnName = "customerId")
  private Customer customer;

  @ElementCollection(fetch = FetchType.LAZY)
  @Builder.Default
  private Set<UserRole> roleSet = new HashSet<>();

  public void addRole(UserRole userRole) {
    this.roleSet.add(userRole);
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Level> levelSet = new HashSet<>();

  public void addLevel(Level level) {
    this.levelSet.add(level);
  }

}
