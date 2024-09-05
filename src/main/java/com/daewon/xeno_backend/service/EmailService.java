package com.daewon.xeno_backend.service;

import jakarta.mail.MessagingException;

public interface EmailService {

    void sendApprovalEmail(String to, String brandName) throws MessagingException;
}
