package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.Reply;
import com.daewon.xeno_backend.domain.Review;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.reply.ReplyDTO;
import com.daewon.xeno_backend.dto.reply.ReplyReadDTO;
import com.daewon.xeno_backend.dto.reply.ReplyUpdateDTO;
import com.daewon.xeno_backend.repository.ReplyRepository;
import com.daewon.xeno_backend.repository.ReviewRepository;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class ReplyServiceImpl implements ReplyService {

    private final ReplyRepository replyRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public Long createReply(ReplyDTO replyDTO) { // 답글 등록
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserName = authentication.getName();

        Users users = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));

        Reply reply = Reply.builder()
                .users(users)

                .text(replyDTO.getReplyText())
                .build();
        reply.setReview(replyDTO.getReviewId());

        Reply savedReply = replyRepository.save(reply);


        return savedReply.getReplyId();
    }

    @Override
    public ReplyReadDTO readReply(Long replyId) {
        Optional<Reply> replyOptional = replyRepository.findById(replyId);
        Reply reply = replyOptional.orElseThrow();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        log.info("이름:"+currentUserName);
        Users users = userRepository.findByEmail(currentUserName).orElse(null);

        if(users != null) {
            ReplyReadDTO replyReadDTO = ReplyReadDTO.builder()
                    .replyId(reply.getReplyId())
                    .userName(reply.getUsers().getName())
                    .replyText(reply.getText())
                    .createAt(reply.getCreateAt() != null ? reply.getCreateAt().format(formatter) : null)
                    .updateAt(reply.getUpdateAt() != null ? reply.getUpdateAt().format(formatter) : null)
                    .isReply(reply.getUsers().getUserId() == users.getUserId())
                    .build();

            return replyReadDTO;
        } else {
            ReplyReadDTO replyReadDTO = ReplyReadDTO.builder()
                    .replyId(reply.getReplyId())
                    .userName(reply.getUsers().getName())
                    .replyText(reply.getText())
                    .createAt(reply.getCreateAt() != null ? reply.getCreateAt().format(formatter) : null)
                    .updateAt(reply.getUpdateAt() != null ? reply.getUpdateAt().format(formatter) : null)
                    .isReply(false)
                    .build();

            return replyReadDTO;
        }
    }

    @Override
    public List<ReplyReadDTO> readReplys(Long reviewId) {
        List<Reply> replies = replyRepository.listOfReview(reviewId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        log.info("이름:" + currentUserName);
        Users users = userRepository.findByEmail(currentUserName)
                .orElse(null);
        List<ReplyReadDTO> replyDTOList = new ArrayList<>();
        if (users != null) {
            replyDTOList = replies.stream()
                    .map(reply -> ReplyReadDTO.builder()
                            .replyId(reply.getReplyId())
                            .userName(reply.getUsers().getName())
                            .replyText(reply.getText())
                            .createAt(reply.getCreateAt() != null ? reply.getCreateAt().format(formatter) : null)
                            .updateAt(reply.getUpdateAt() != null ? reply.getUpdateAt().format(formatter) : null)
                            .isReply(reply.getUsers().getUserId() == users.getUserId())
                            .build())
                    .collect(Collectors.toList());
        } else {
            replyDTOList = replies.stream()
                    .map(reply -> ReplyReadDTO.builder()
                            .replyId(reply.getReplyId())
                            .userName(reply.getUsers().getName())
                            .replyText(reply.getText())
                            .createAt(reply.getCreateAt() != null ? reply.getCreateAt().format(formatter) : null)
                            .updateAt(reply.getUpdateAt() != null ? reply.getUpdateAt().format(formatter) : null)
                            .isReply(false)
                            .build())
                    .collect(Collectors.toList());
        }
        return replyDTOList;
    }

    @Override
    public void updateReply(ReplyUpdateDTO replyUpdateDTO) { // 댓글 수정
        Optional<Reply> replyOptional = replyRepository.findById(replyUpdateDTO.getReplyId());
        Reply reply = replyOptional.orElseThrow();

        reply.setReplyText(replyUpdateDTO.getReplyText()); // 리뷰 내용 수정

        replyRepository.save(reply);
    }

    @Override
    public void deleteReply(Long replyId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없음"));
        
        // userId를 1로 가정
        Long assumedUserId = 1L;

        if (reply.getUsers().getUserId() != assumedUserId) {
            throw new RuntimeException("댓글을 수정할 권한이 없습니다.");
        }

        Review review = reply.getReview();

        replyRepository.deleteById(replyId);
    }

    // 사용자 ID로 댓글 목록을 조회하는 메서드
    @Override
    public List<ReplyReadDTO> getRepliesByUserId(Long userId) {
        List<Reply> replies = replyRepository.findByUserId(userId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        log.info("이름:" + currentUserName);
        Users users = userRepository.findByEmail(currentUserName)
                .orElse(null);
        List<ReplyReadDTO> replyDTOList = new ArrayList<>();
        if (users != null) {
            replies.stream()
                    .map(reply -> ReplyReadDTO.builder()
                            .replyId(reply.getReplyId())
                            .userName(reply.getUsers().getName())
                            .replyText(reply.getText())
                            .createAt(reply.getCreateAt() != null ? reply.getCreateAt().format(formatter) : null)
                            .updateAt(reply.getUpdateAt() != null ? reply.getUpdateAt().format(formatter) : null)
                            .isReply(reply.getUsers().getUserId() == users.getUserId())
                            .build())
                    .collect(Collectors.toList());
        } else {
            replies.stream()
                    .map(reply -> ReplyReadDTO.builder()
                            .replyId(reply.getReplyId())
                            .userName(reply.getUsers().getName())
                            .replyText(reply.getText())
                            .createAt(reply.getCreateAt() != null ? reply.getCreateAt().format(formatter) : null)
                            .updateAt(reply.getUpdateAt() != null ? reply.getUpdateAt().format(formatter) : null)
                            .isReply(false)
                            .build())
                    .collect(Collectors.toList());
        }
        return replyDTOList;
    }
}