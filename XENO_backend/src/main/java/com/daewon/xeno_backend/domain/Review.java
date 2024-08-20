package com.daewon.xeno_backend.domain;


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
public class Review extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment와 같은
  private long reviewId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "userId", referencedColumnName = "userId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Users users;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "orderId", referencedColumnName = "orderId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Orders order;

  private String text;

  private double star;

  public void setUsers(Long userId) {
    this.users = Users.builder().userId(userId).build();
  }

  public void setOrders(Long orderId) {
    this.order = Orders.builder().orderId(orderId).build();
  }


}
