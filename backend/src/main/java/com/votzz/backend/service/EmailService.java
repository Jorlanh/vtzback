package com.votzz.backend.service;

import io.awspring.cloud.ses.SimpleEmailServiceMailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    // Injeção opcional: se o bean não existir (configuração AWS falhar), será null
    @Autowired(required = false)
    private SimpleEmailServiceMailSender mailSender;

    @Value("${aws.ses.sender-email:no-reply@votzz.com}")
    private String senderEmail;

    public void sendSimpleEmail(String to, String subject, String content) {
        if (mailSender == null) {
            log.warn("AWS SES não configurado. E-mail NÃO enviado para: {}", to);
            log.info("Conteúdo do E-mail:\nSubject: {}\nBody: {}", subject, content);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("E-mail enviado com sucesso via SES para: {}", to);
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail via SES: {}", e.getMessage());
        }
    }
    
    public void sendResetToken(String to, String token) {
        sendSimpleEmail(to, "Recuperação de Senha - Votzz", "Seu código é: " + token);
    }
}