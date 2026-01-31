package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.repository.*;
import com.votzz.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j 
public class WebhookController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository; // Injetado para buscar reservas
    private final PaymentService paymentService;

    @Value("${asaas.webhook.token}") 
    private String asaasWebhookToken;

    @Value("${votzz.kiwify.webhook.token}")
    private String kiwifyWebhookToken;

    @PostMapping("/asaas")
    @Transactional // Garante que a atualização no banco seja atômica
    public ResponseEntity<String> handleAsaas(@RequestBody Map<String, Object> payload, 
                                              @RequestHeader(value = "asaas-access-token", required = false) String token) {
        
        // 1. Validação de Segurança (Opcional: Verificar token se configurado)
        // Nota: Em multi-tenant, cada condomínio pode ter seu token. 
        // Se você usar um token fixo no app.properties, ele só valida o SEU webhook principal.
        // Para simplificar e permitir que funcione para condomínios sem configurar token individual, 
        // deixamos passar se o token bater com o mestre OU se não for exigido rigorosamente aqui.
        // Se quiser rigor, teria que buscar o Tenant pela cobrança primeiro para validar o token dele.
        
        if (token != null && !token.isEmpty() && !token.equals(asaasWebhookToken)) {
             // Se veio um token e não é o nosso mestre, logamos warning mas não bloqueamos 
             // pois pode ser o token de um condomínio específico que não temos como validar sem saber quem é.
             log.warn("Webhook Asaas com token diferente do mestre. Pode ser de um tenant específico.");
        }

        String event = (String) payload.get("event");
        
        // Se o pagamento foi confirmado ou recebido
        if ("PAYMENT_CONFIRMED".equals(event) || "PAYMENT_RECEIVED".equals(event)) {
            
            Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
            
            String asaasCustomerId = (String) payment.get("customer");
            String paymentId = (String) payment.get("id"); // ID da cobrança (pay_123456)

            log.info("Webhook Asaas recebido. Evento: {}, PaymentId: {}", event, paymentId);

            // --- CENÁRIO A: PAGAMENTO DE RESERVA DE ÁREA (Booking) ---
            // Tenta encontrar uma reserva com este ID de pagamento
            Booking booking = bookingRepository.findByAsaasPaymentId(paymentId);
            
            if (booking != null) {
                // Confirma a reserva automaticamente
                if (!"CONFIRMED".equals(booking.getStatus())) {
                    booking.setStatus("CONFIRMED");
                    bookingRepository.save(booking);
                    log.info("Reserva #{} confirmada automaticamente via Webhook Asaas.", booking.getId());
                }
                return ResponseEntity.ok("OK - Booking Confirmed");
            }

            // --- CENÁRIO B: ASSINATURA DA PLATAFORMA (SaaS) ---
            // Se não for reserva, tenta achar o Tenant pelo Customer ID para ativar o plano
            tenantRepository.findByAsaasCustomerId(asaasCustomerId).ifPresent(tenant -> {
                paymentService.processSubscriptionActivation(tenant, 1);
                log.info("Condomínio '{}' ativado com sucesso via Asaas (Assinatura).", tenant.getNome());
            });
        }
        
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/kiwify")
    public ResponseEntity<String> handleKiwify(@RequestBody Map<String, Object> payload,
                                               @RequestParam("token") String token) {
        // Validação de segurança do token Kiwify
        if (!kiwifyWebhookToken.equals(token)) {
            return ResponseEntity.status(401).build();
        }

        String status = (String) payload.get("order_status");
        
        if ("paid".equalsIgnoreCase(status)) {
            Map<String, Object> customer = (Map<String, Object>) payload.get("Customer");
            String email = (String) customer.get("email");
            
            log.info("Webhook Kiwify recebido. Email: {}", email);
            
            userRepository.findByEmail(email).ifPresent(user -> {
                if (user.getTenant() != null) {
                    paymentService.processSubscriptionActivation(user.getTenant(), 1);
                    log.info("Condomínio do usuário '{}' ativado via Kiwify.", user.getNome());
                }
            });
        }
        return ResponseEntity.ok("OK");
    }
}