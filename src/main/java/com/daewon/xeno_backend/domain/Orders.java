package com.daewon.xeno_backend.domain;


import com.daewon.xeno_backend.domain.auth.Brand;
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
public class Orders extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long orderId;

  @Column(length = 64, nullable = false)
  private String orderPayId;

  @ManyToOne()  // fetch = FetchType.LAZY
  @JoinColumn(name = "productOptionId", referencedColumnName = "productOptionId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private ProductsOption productsOption;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customerId", referencedColumnName = "customerId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Users customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brandId", referencedColumnName = "brandId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Brand brand;


  @Column(nullable = false)
  private Long orderNumber;

  private String paymentKey;

  private String status;

  // 고객의 요청사항
  private String req;

  private int quantity;

  // 총 합 가격
  private Long amount;

  private int usePoint;
}
