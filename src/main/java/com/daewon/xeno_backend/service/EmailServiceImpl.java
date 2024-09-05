package com.daewon.xeno_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.naming.Context;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;


    public void sendApprovalEmail(String to, String brandName) throws MessagingException {
        // MimeMessage 생성: 다양한 형식(HTML, 첨부 파일 등)을 지원하는 메시지 객체
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        // MimeMessageHelper: MimeMessage 생성을 돕는 헬퍼 클래스
        // 첫 번째 인자: MimeMessage 객체
        // 두 번째 인자: multipart 모드 사용 여부 (true: 첨부 파일 지원)
        // 세 번째 인자: 문자 인코딩 (한글 지원을 위해 UTF-8 사용)
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

        // 발신자 이메일 설정
        helper.setFrom("myEmail@gmail.com");
        // 수신자 이메일 설정
        helper.setTo(to);
        // 이메일 제목 설정
        helper.setSubject(brandName + " 브랜드 가입 승인 완료");

        // HTML 형식의 이메일 본문 생성
        String htmlContent = "<html>"
                + "<head><title>브랜드 가입 승인</title></head>"
                + "<body>"
                + "<h1>" + brandName + " 브랜드 가입 승인 완료</h1>"
                + "<p>안녕하세요,</p>"
                + "<p><strong>" + brandName + "</strong> 브랜드의 가입이 승인되었습니다.</p>"
                + "<p>이제 서비스를 이용하실 수 있습니다.</p>"
                + "<p>감사합니다.</p>"
                + "</body>"
                + "</html>";

        // 이메일 본문 설정
        // 첫 번째 인자: 이메일 본문
        // 두 번째 인자: HTML 형식 사용 여부 (true: HTML 형식 사용)
        helper.setText(htmlContent, true);

        // 이메일 전송
        mailSender.send(mimeMessage);
    }
}
