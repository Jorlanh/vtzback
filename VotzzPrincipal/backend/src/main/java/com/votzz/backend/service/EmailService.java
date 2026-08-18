package com.votzz.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}") 
    private String senderEmail;

    /**
     * Método genérico para enviar HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Define o remetente oficial
            helper.setFrom(senderEmail, "Votzz - Assembleias Digitais");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indica que o conteúdo é HTML

            mailSender.send(message);
            log.info("E-mail enviado para: {}", to);
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail para {}: {}", to, e.getMessage());
        }
    }

    /**
     * Notifica moradores com layout profissional, botão e nome do condomínio.
     */
    @Async
    public void sendNewAssemblyNotification(List<String> recipients, String title, String description, String periodo, String link, String tenantName) {
        if (recipients == null || recipients.isEmpty()) {
            log.info("Lista de destinatários vazia.");
            return;
        }

        // 1. Assunto Personalizado
        String subject = String.format("%s - 📢 Convocação: %s", tenantName, title);
        
        // 2. Template HTML com Botão
        String body = String.format(
            "<div style='font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto; line-height: 1.6; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>" +
                
                // Cabeçalho
                "<div style='background-color: #10b981; padding: 20px; text-align: center;'>" +
                    "<h2 style='color: white; margin: 0;'>Convocação de Assembleia</h2>" +
                    "<p style='color: #ecfdf5; margin: 5px 0 0 0;'>%s</p>" +
                "</div>" +

                // Conteúdo
                "<div style='padding: 30px;'>" +
                    "<p>Olá vizinho(a),</p>" +
                    "<p>Você está sendo convocado(a) para participar de uma assembleia oficial no nosso condomínio.</p>" +
                    
                    "<div style='background: #f8fafc; padding: 20px; border-radius: 8px; border-left: 4px solid #10b981; margin: 20px 0;'>" +
                        "<p style='margin: 0 0 10px 0;'><strong>📌 TÍTULO:</strong> <br/><span style='color: #475569;'>%s</span></p>" +
                        "<p style='margin: 0 0 10px 0;'><strong>📝 PAUTA:</strong> <br/><span style='color: #475569;'>%s</span></p>" +
                        "<p style='margin: 0;'><strong>📅 PERÍODO:</strong> <br/><span style='color: #475569;'>%s</span></p>" +
                    "</div>" +

                    // Botão Clicável
                    "<div style='text-align: center; margin: 40px 0;'>" +
                        "<a href='%s' style='background-color: #2563eb; color: white; padding: 15px 30px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; display: inline-block;'>CLIQUE AQUI PARA ACESSAR A SALA</a>" +
                    "</div>" +

                    "<p style='font-size: 13px; color: #94a3b8;'>Se o botão acima não funcionar, copie e cole este link no seu navegador:<br/>" +
                    "<a href='%s' style='color: #2563eb;'>%s</a></p>" +
                "</div>" +

                // Rodapé / Assinatura
                "<div style='background-color: #f1f5f9; padding: 20px; text-align: center; font-size: 12px; color: #64748b;'>" +
                    "<p style='margin: 0;'>Atenciosamente,</p>" +
                    "<p style='margin: 5px 0 0 0;'><strong>Administração do Condomínio \"%s\" via Votzz.</strong></p>" +
                "</div>" +
            "</div>",
            tenantName, // Cabeçalho Verde
            title, 
            (description != null ? description : "Ver pauta completa no sistema"), 
            periodo, 
            link, // Link do Botão
            link, link, // Link texto backup
            tenantName // Assinatura
        );

        log.info("Iniciando disparo para {} moradores do condomínio {}.", recipients.size(), tenantName);

        for (String email : recipients) {
            sendHtmlEmail(email, subject, body);
        }
    }
    
    // Sobrecarga para compatibilidade (Emails automáticos de criação)
    @Async
    public void sendNewAssemblyNotification(List<String> recipients, String title, String description, String periodo, String link) {
        sendNewAssemblyNotification(recipients, title, description, periodo, link, "Seu Condomínio");
    }

    @Async
    public void sendGenericNotification(List<String> recipients, String subject, String content) {
        if (recipients == null || recipients.isEmpty()) return;
        
        String body = "<div style='font-family: Arial;'>" + content + 
                      "<br><br>Atenciosamente,<br><strong>Administração via Votzz.</strong></div>";
        
        for (String email : recipients) {
            sendHtmlEmail(email, subject, body);
        }
    }

    public void sendResetToken(String to, String token) {
        sendHtmlEmail(to, "Recuperação de Senha - Votzz", 
            "<h3>Recuperação de Senha</h3><p>Seu código é: <strong>" + token + "</strong></p>");
    }
}