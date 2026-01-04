package com.votzz.backend.service;

import io.awspring.cloud.ses.SimpleEmailServiceMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmailService {

    // Injeção opcional: se o bean não existir (configuração AWS falhar), será null
    @Autowired(required = false)
    private SimpleEmailServiceMailSender mailSender;

    @Value("${aws.ses.sender-email}") 
    private String senderEmail;

    /**
     * Envia um e-mail simples para um único destinatário
     */
    public void sendSimpleEmail(String to, String subject, String content) {
        if (mailSender == null) {
            log.warn("AWS SES não configurado. E-mail NÃO enviado para: {}", to);
            log.info("Simulação de E-mail:\nTo: {}\nSubject: {}\nBody: {}", to, subject, content);
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
            log.error("Erro ao enviar e-mail via SES para {}: {}", to, e.getMessage());
        }
    }

    /**
     * Notifica todos os moradores sobre uma nova assembleia.
     * @Async garante que o síndico não fique esperando o envio de centenas de e-mails para ver a tela de sucesso.
     */
    @Async
    public void sendNewAssemblyNotification(List<String> recipients, String title, String date) {
        if (recipients == null || recipients.isEmpty()) {
            log.info("Nenhum destinatário encontrado para a assembleia: {}", title);
            return;
        }

        String subject = "Nova Assembleia Agendada: " + title;
        String body = String.format(
            "Olá!\n\nUma nova assembleia foi agendada no seu condomínio.\n\n" +
            "Tema: %s\n" +
            "Data de Início: %s\n\n" +
            "Acesse a plataforma Votzz para conferir a pauta completa e participar da votação digital.\n\n" +
            "Atenciosamente,\nEquipe Votzz.",
            title, date
        );

        log.info("Iniciando disparo de e-mails para {} moradores sobre a assembleia: {}", recipients.size(), title);

        for (String email : recipients) {
            sendSimpleEmail(email, subject, body);
        }
    }
    
    public void sendResetToken(String to, String token) {
        sendSimpleEmail(to, "Recuperação de Senha - Votzz", "Seu código de recuperação é: " + token);
    }
}