package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.repository.*;
import com.votzz.backend.service.EmailService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final TenantRepository tenantRepository;
    private final ComissaoRepository comissaoRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${asaas.webhook.token}") 
    private String asaasWebhookToken;

    @Value("${votzz.kiwify.webhook.token}")
    private String kiwifyWebhookToken;

    // --- 1. WEBHOOK DO ASAAS (Reservas & Taxas) ---
    @PostMapping("/asaas")
    @Transactional
    public ResponseEntity<String> handleAsaasWebhook(@RequestBody AsaasWebhookEvent event, 
                                                     @RequestHeader(value = "asaas-access-token", required = false) String token) {
        
        if (asaasWebhookToken != null && !asaasWebhookToken.equals(token)) {
            return ResponseEntity.status(401).body("Acesso Negado");
        }

        if ("PAYMENT_CONFIRMED".equals(event.event) || "PAYMENT_RECEIVED".equals(event.event)) {
            processarReserva(event.payment);
        } else if ("PAYMENT_REFUNDED".equals(event.event)) {
            cancelarReserva(event.payment);
        }
        return ResponseEntity.ok("Asaas Processed");
    }

    // --- 2. WEBHOOK DO KIWIFY (Assinaturas do Plano) ---
    @PostMapping("/kiwify")
    @Transactional
    public ResponseEntity<String> handleKiwifyWebhook(@RequestBody Map<String, Object> payload,
                                                      @RequestParam(required = false) String token) {
        
        // Verifica token se passado na URL (opcional)
        if (kiwifyWebhookToken != null && !kiwifyWebhookToken.equals(token)) {
             // log.warn("Token Kiwify inválido"); 
        }

        try {
            String orderStatus = (String) payload.get("order_status"); // 'paid'
            Map<String, Object> customer = (Map<String, Object>) payload.get("Customer");
            String email = (String) customer.get("email");

            log.info("Kiwify Webhook: Status={} Email={}", orderStatus, email);

            if ("paid".equalsIgnoreCase(orderStatus)) {
                User user = userRepository.findByEmail(email).orElse(null);
                
                if (user != null && user.getTenant() != null) {
                    Tenant tenant = user.getTenant();
                    
                    // Lógica de Renovação Automática
                    tenant.renovarAssinatura(1); // Adiciona 1 mês
                    tenant.setStatusAssinatura("ACTIVE");
                    tenantRepository.save(tenant);
                    
                    log.info("ACESSO LIBERADO: Condomínio {} ativado.", tenant.getNome());
                    
                    // Disparar Email
                    try {
                        emailService.sendSimpleEmail(
                            email, 
                            "Acesso Liberado - Votzz", 
                            "Olá " + user.getNome() + ",\n\nSeu pagamento foi confirmado e seu acesso ao Votzz está liberado!\n\nAcesse: https://app.votzz.com"
                        );
                    } catch (Exception e) { log.error("Erro ao enviar email", e); }
                }
            }
            return ResponseEntity.ok("Kiwify Processed");
        } catch (Exception e) {
            log.error("Erro Kiwify", e);
            return ResponseEntity.ok("Error but received");
        }
    }

    // --- LÓGICA DE NEGÓCIO ASAAS ---
    private void processarReserva(AsaasPaymentData payment) {
        Booking booking = bookingRepository.findByAsaasPaymentId(payment.id);
        if (booking != null) {
            booking.setStatus("APPROVED");
            bookingRepository.save(booking);
        }
    }

    private void cancelarReserva(AsaasPaymentData payment) {
        Booking booking = bookingRepository.findByAsaasPaymentId(payment.id);
        if (booking != null) {
            booking.setStatus("REJECTED");
            bookingRepository.save(booking);
        }
    }

    // DTOs Internos Asaas
    @Data public static class AsaasWebhookEvent { public String event; public AsaasPaymentData payment; }
    @Data public static class AsaasPaymentData { public String id; public String customer; public BigDecimal value; }
}