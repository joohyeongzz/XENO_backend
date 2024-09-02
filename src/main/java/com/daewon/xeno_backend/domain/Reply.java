package com.daewon.xeno_backend.domain;


import com.daewon.xeno_backend.domain.auth.Users;
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
public class Reply extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment와 같은
  private long replyId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "userId", referencedColumnName = "userId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Users users;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewId", referencedColumnName = "reviewId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Review review;

  @Column(length = 255)
  private String text;

  // 리뷰 댓글 작성 내용 수정
  public void setReplyText(String text) { this.text = text; }

  // pharmacy 값 설정 -> phId를 받아서 생성    // pharmacy 값 설정하는거 맞나요?
  // review 값 설정 -> reviewId를 받아서 생성
  public void setReview(Long reviewId) {
    this.review = review.builder().reviewId(reviewId).build();
  }

  // users 값 설정 -> userId를 받아서 생성
  public void setUsers(Long userId) {
    this.users = Users.builder().userId(userId).build();
  }




}
