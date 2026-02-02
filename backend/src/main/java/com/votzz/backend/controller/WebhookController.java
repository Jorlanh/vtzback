package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.repository.*;
import com.votzz.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Adicionado para logs
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j // Importante para ver se o webhook chegou no console
public class WebhookController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Value("${asaas.webhook.token}") 
    private String asaasWebhookToken;

    @Value("${votzz.kiwify.webhook.token}")
    private String kiwifyWebhookToken;

    @PostMapping("/asaas")
    public ResponseEntity<String> handleAsaas(@RequestBody Map<String, Object> payload, 
                                              @RequestHeader(value = "asaas-access-token", required = false) String token) {
        // Validação de segurança do token Asaas
        if (token == null || !asaasWebhookToken.equals(token)) {
            return ResponseEntity.status(401).build();
        }

        String event = (String) payload.get("event");
        
        // Se o pagamento foi confirmado
        if ("PAYMENT_CONFIRMED".equals(event)) {
            // Extrai o objeto de pagamento
            Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
            
            // Pega o ID do cliente no Asaas (ex: cus_000005105260)
            String asaasCustomerId = (String) payment.get("customer");

            log.info("Webhook Asaas recebido. Evento: {}, Cliente: {}", event, asaasCustomerId);

            // Busca o condomínio pelo ID do Asaas e ativa
            tenantRepository.findByAsaasCustomerId(asaasCustomerId).ifPresent(tenant -> {
                // Ativa por 1 mês (ou ajuste conforme sua regra de ciclo se tiver essa info no payload)
                paymentService.processSubscriptionActivation(tenant, 1);
                log.info("Condomínio '{}' ativado com sucesso via Asaas.", tenant.getNome());
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