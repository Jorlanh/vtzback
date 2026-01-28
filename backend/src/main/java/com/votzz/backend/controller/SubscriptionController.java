package com.votzz.backend.controller;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Retorna se o condomínio precisa de renovação (Front usa isso para mostrar o alerta)
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal User user) {
        if (user.getTenant() == null) {
            return ResponseEntity.badRequest().body("Usuário não vinculado a um condomínio.");
        }
        return ResponseEntity.ok(subscriptionService.checkRenewalStatus(user.getTenant()));
    }

    /**
     * Endpoint unificado para criar pagamento (Renovação ou Upgrade)
     */
    @PostMapping("/create-checkout")
    public ResponseEntity<?> createCheckout(@RequestBody Map<String, Object> payload, @AuthenticationPrincipal User user) {
        Tenant tenant = user.getTenant();
        if (tenant == null) return ResponseEntity.badRequest().body("Sem condomínio vinculado.");

        String planType = (String) payload.getOrDefault("planType", tenant.getPlano().getNome());
        String cycle = (String) payload.getOrDefault("cycle", "ANUAL");
        
        // Se o front não mandar unidades, usa a quantidade atual do condomínio
        int units = payload.get("units") != null ? (int) payload.get("units") : tenant.getUnidadesTotal();

        try {
            Map<String, Object> checkoutData = subscriptionService.createCheckout(
                planType, 
                cycle, 
                units, 
                tenant, 
                user.getEmail(), 
                user.getWhatsapp()
            );
            return ResponseEntity.ok(checkoutData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Renovação manual (Uso administrativo ou via webhook de confirmação)
     */
    @PostMapping("/renew-manual")
    public ResponseEntity<String> renewManual(@RequestBody Map<String, Integer> payload, @AuthenticationPrincipal User user) {
        if (user.getTenant() == null) return ResponseEntity.badRequest().body("Erro de contexto.");
        
        int months = payload.getOrDefault("months", 12);
        subscriptionService.renewSubscription(user.getTenant(), months);
        return ResponseEntity.ok("Assinatura renovada com sucesso!");
    }
}