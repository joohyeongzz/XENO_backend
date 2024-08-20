package com.daewon.xeno_backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@Table(name = "cart")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartId")
    private Long cartId;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productColorSizeId",  referencedColumnName = "productColorSizeId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ProductsColorSize productsColorSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productImageId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ProductsImage productsImage;

    @Column(name = "quantity", nullable = false)
    private Long quantity;  // 수량

    @Column(name = "price", nullable = false)
    private Long price;

//    @Builder.Default
//    @Column(name = "selected")
//    private boolean selected = true;   // 선택 여부

//    public Cart(Users user, ProductsColorSize productsColorSize, ProductsImage productsImage, Long quantity, Long price) {
//        this.user = user;
//        this.productsColorSize = productsColorSize;
//        this.productsImage = productsImage;
//        this.quantity = quantity;
//        this.price = price;
//        this.selected = true;   // 기본적으로 선택된 상태로 추가
//    }

//    public Cart() {
//
//    }
}