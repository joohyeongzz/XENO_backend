package com.daewon.xeno_backend.domain;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Products {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment와 같은

  private long productId;

  private String brandName;

  private String name;

  private String category;

  private String categorySub;

  private long price;

  private long priceSale;

  private boolean isSale;

  private String productNumber;

  private String season;

//  // ProductsColor와의 연관 관계 설정
//  @OneToMany(mappedBy = "products", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//  private List<ProductsColor> colors = new ArrayList<>();

  public boolean getIsSale() {
    return isSale;
  }

  public void setIsSale(boolean isSale) {
      this.isSale = isSale;
  }
}
