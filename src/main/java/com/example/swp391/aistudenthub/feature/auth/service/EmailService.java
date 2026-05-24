package com.example.swp391.aistudenthub.feature.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[AI Study Hub] Đặt lại mật khẩu");
        message.setText(
                "Xin chào,\n\n" +
                "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n\n" +
                "Nhấn vào link bên dưới để đặt lại mật khẩu (link có hiệu lực trong 1 giờ):\n\n" +
                resetLink + "\n\n" +
                "Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email này.\n\n" +
                "Trân trọng,\nAI Study Hub"
        );

        mailSender.send(message);
    }
}
