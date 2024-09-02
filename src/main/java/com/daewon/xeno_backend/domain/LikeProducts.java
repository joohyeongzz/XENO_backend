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
public class LikeProducts {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment와 같은
  private long likeId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "userId", referencedColumnName = "userId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Users users;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "productLikeId", referencedColumnName = "productLikeId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private ProductsLike productsLike;

  private boolean isLike;

  public LikeProducts(ProductsLike productsLike, Users users) {
    this.productsLike = productsLike;
    this.users = users;
    this.isLike = true;
  }

}
