package com.daewon.xeno_backend.domain;

import com.daewon.xeno_backend.domain.auth.Users;
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
public class UploadImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment와 같은
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "userId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users users;

    private String productNumber;

    private String url_1;
    private String url_2;
    private String url_3;
    private String url_4;
    private String url_5;
    private String url_6;
    private String detail_url_1;

}
