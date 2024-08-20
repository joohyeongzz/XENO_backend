package com.daewon.xeno_backend.dto.reply;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyReadDTO {

    private Long replyId;

    private String userName;

    @NotEmpty
    private String replyText;

    private boolean isReply;

    private String createAt;

    private String updateAt;

}
