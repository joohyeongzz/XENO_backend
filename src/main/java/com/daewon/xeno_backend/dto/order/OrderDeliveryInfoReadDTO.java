package com.daewon.xeno_backend.dto.order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeliveryInfoReadDTO {
    private String address;
    private String phoneNumber;
    private String req;
}
