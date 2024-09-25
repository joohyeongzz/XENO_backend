
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
public class ProductsOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment와 같은
    private long productOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", referencedColumnName = "productId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Products products;

    private String size;

    private long stock;

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new RuntimeException("SoldOutException 발생. 주문한 상품량이 재고량보다 큽니다.");
        }
        this.stock -= quantity;
    }


}

