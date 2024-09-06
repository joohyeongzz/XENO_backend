package com.daewon.xeno_backend.dto.order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductIdsReadDTO {
    private Long productOptionId;
    private String productName;
    private String color;
    private String size;
    private Long price;
    private String productImage;
    private Long quantity;
    private Long productId;
}
