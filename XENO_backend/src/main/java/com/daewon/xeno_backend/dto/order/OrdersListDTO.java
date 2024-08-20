package com.daewon.xeno_backend.dto.order;

import com.daewon.xeno_backend.dto.auth.GetOneDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersListDTO {

    private String req;
    private Long ProductColorSizeId;
    private Long orderNumber;
    private LocalDateTime orderDate;
    private String brandName;
    private String status;
    private Long amount;
    private int quantity;

    private List<GetOneDTO> getOne;
}
