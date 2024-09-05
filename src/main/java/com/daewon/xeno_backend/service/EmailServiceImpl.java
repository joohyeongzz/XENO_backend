package com.daewon.xeno_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public void sendApprovalEmail(String to, String brandName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("브랜드 가입 승인 완료");
        message.setText("안녕하세요,\n\n" +
                brandName + " 브랜드의 가입이 승인되었습니다.\n" +
                "이제 서비스를 이용하실 수 있습니다.\n\n" +
                "감사합니다.");

        mailSender.send(message);
    }
}
