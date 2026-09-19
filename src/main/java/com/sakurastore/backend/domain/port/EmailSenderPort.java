package com.sakurastore.backend.domain.port;

public interface EmailSenderPort {
    void sendVerificationCode(String toEmail, String recipientName, String code);
}
