package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.dto.reply.ReplyDTO;
import com.daewon.xeno_backend.dto.reply.ReplyReadDTO;
import com.daewon.xeno_backend.dto.reply.ReplyUpdateDTO;
import com.daewon.xeno_backend.service.ReplyService;
import com.daewon.xeno_backend.utils.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequestMapping("api/reply")
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;
    private final JWTUtil jwtUtil;

    // @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Replies Post")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Long createReply(@RequestBody ReplyDTO replyDTO) {
        log.info(replyDTO);
        Long replyId;
        try {
            replyId = replyService.createReply(replyDTO);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad Request", e);
        }
        return replyId;
    }

    // 작성한 유저만 삭제 가능
    @PreAuthorize("@reviewAndReplySecurityUtils.isReplyOwner(#replyId)")
    @DeleteMapping()
    public Map<String, String> deleteReply(@RequestParam(name = "replyId") Long replyId) {
        replyService.deleteReply(replyId);
        return Map.of("result", "삭제 완료");
    }

    // 작성한 유저만 수정 가능
    @PreAuthorize("@reviewAndReplySecurityUtils.isReplyOwner(#replyUpdateDTO.replyId)")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> modifyReply(@RequestBody ReplyUpdateDTO replyUpdateDTO) {
        log.info(replyUpdateDTO);
        replyService.updateReply(replyUpdateDTO);
        return Map.of("result", "수정완료");
    }

    // @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Replies Post")
    @GetMapping("/list")
    public List<ReplyReadDTO> readReplys(
            @RequestParam(name = "reviewId") Long reviewId) {
        List<ReplyReadDTO> replyReadDTO = replyService.readReplys(reviewId);

        return replyReadDTO;
    }

    // @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Replies Post")
    @GetMapping("/read")
    public ReplyReadDTO readReply(@RequestParam(name = "replyId") Long replyId) {
        ReplyReadDTO replyReadDTO = replyService.readReply(replyId);

        return replyReadDTO;
    }
}

/*
    createReply

    {
      "reviewId": 해당하는 reviewId,
      "replyText": "This is a test reply."
    }

    updateReply

    {
      "replyId": 해당하는 replyId,
      "replyText": "siuuuuuu"
    }
 */
