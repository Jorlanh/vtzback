package com.votzz.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendResetToken(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Votzz - Recuperação de Senha");
        message.setText("Seu código de verificação é: " + token + "\n\nEste código expira em 15 minutos.");
        message.setFrom("nao-responda@votzz.com");

        mailSender.send(message);
    }
}