package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.repository.*;
import com.votzz.backend.service.PaymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
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
                                              @RequestHeader("asaas-access-token") String token) {
        if (!asaasWebhookToken.equals(token)) return ResponseEntity.status(401).build();

        String event = (String) payload.get("event");
        if ("PAYMENT_CONFIRMED".equals(event)) {
            // Lógica: buscar Tenant pelo externalId e ativar
        }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/kiwify")
    public ResponseEntity<String> handleKiwify(@RequestBody Map<String, Object> payload,
                                               @RequestParam("token") String token) {
        if (!kiwifyWebhookToken.equals(token)) return ResponseEntity.status(401).build();

        String status = (String) payload.get("order_status");
        if ("paid".equalsIgnoreCase(status)) {
            Map<String, Object> customer = (Map<String, Object>) payload.get("Customer");
            String email = (String) customer.get("email");
            
            userRepository.findByEmail(email).ifPresent(user -> {
                paymentService.processSubscriptionActivation(user.getTenant(), 1);
            });
        }
        return ResponseEntity.ok("OK");
    }
}