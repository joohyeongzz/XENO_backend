package com.daewon.xeno_backend.domain.auth;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Customer {

    @Id
    private Long customerId;

    @OneToOne(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Users user;

    private Long point;

    @Enumerated(EnumType.STRING)
    private Level level;
}
