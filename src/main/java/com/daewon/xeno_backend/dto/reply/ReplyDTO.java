package com.daewon.xeno_backend.dto.reply;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyDTO {

    private Long replyId;

    private Long reviewId;

    private String userName;    // 특정한 유저 id를 선언

    @NotEmpty
    private String replyText;

    private LocalDateTime createAt;

    private LocalDateTime updateAt;

}
