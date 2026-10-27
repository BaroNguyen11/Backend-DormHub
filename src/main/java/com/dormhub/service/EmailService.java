
package com.dormhub.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendVerificationOtp(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(toEmail);
        message.setSubject("DormHub - Xác minh email");

        message.setText("""
                Xin chào,

                Bạn đã đăng ký tài khoản DormHub.

                Mã OTP xác minh email của bạn là: %s

                Mã OTP có hiệu lực trong 5 phút.
                Vui lòng không chia sẻ mã này với bất kỳ ai.

                Nếu bạn không thực hiện yêu cầu đăng ký,
                vui lòng bỏ qua email này.

                Trân trọng,
                DormHub
                """.formatted(otp));

        mailSender.send(message);
    }
}
