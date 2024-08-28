package com.daewon.xeno_backend.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UploadImageReadDTO {
    private String productNumber;

    private String url_1;
    private String url_2;
    private String url_3;
    private String url_4;
    private String url_5;
    private String url_6;
    private String detail_url_1;

    private String createdAt;

}
