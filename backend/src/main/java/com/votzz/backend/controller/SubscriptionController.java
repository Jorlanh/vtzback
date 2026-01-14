package com.votzz.backend.controller;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.integration.AsaasClient;
import com.votzz.backend.repository.TenantRepository;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.service.PaymentService;
import com.votzz.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService; // New Service for calculation
    private final AsaasClient asaasClient;
    private final TenantRepository tenantRepository;

    @PostMapping("/renew")
    public ResponseEntity<String> renewSubscription(@RequestBody Map<String, Integer> payload, @AuthenticationPrincipal User user) {
        if (user.getTenant() == null) {
            return ResponseEntity.badRequest().body("Usuário não vinculado a um condomínio.");
        }

        int months = payload.getOrDefault("months", 12);
        
        // This method just updates the date (admin/system action usually, or post-webhook)
        subscriptionService.renewSubscription(user.getTenant(), months);

        return ResponseEntity.ok("Assinatura renovada com sucesso!");
    }

    // --- NOVO ENDPOINT DE CHECKOUT (Para plano Custom) ---
    @PostMapping("/create-checkout")
    public ResponseEntity<?> createCheckout(@RequestBody Map<String, Object> payload, @AuthenticationPrincipal User user) {
        Tenant tenant = user.getTenant();
        if (tenant == null) return ResponseEntity.badRequest().body("Sem condomínio.");

        String planType = (String) payload.getOrDefault("planType", "CUSTOM");
        String cycle = (String) payload.getOrDefault("cycle", "ANUAL");
        int units = (int) payload.getOrDefault("units", 50);

        // 1. Validar unidades com o cadastro real (Opcional, mas seguro)
        // if (tenant.getUnidadesTotal() != null && tenant.getUnidadesTotal() > units) ...

        // 2. Calcular Preço no Servidor
        BigDecimal price;
        if ("CUSTOM".equalsIgnoreCase(planType)) {
            price = paymentService.calculateCustomPlanPrice(units, cycle);
        } else {
            // Logica para outros planos se for processar pelo Asaas também
            return ResponseEntity.badRequest().body("Planos fixos devem usar Kiwify.");
        }

        // 3. Criar/Recuperar Cliente no Asaas
        String asaasId = tenant.getAsaasCustomerId();
        if (asaasId == null) {
            asaasId = asaasClient.createCustomer(tenant.getNome(), tenant.getCnpj(), user.getEmail());
            tenant.setAsaasCustomerId(asaasId);
            tenantRepository.save(tenant);
        }

        // 4. Gerar Cobrança (PIX/Boleto)
        // Retorna o QR Code e o Copia e Cola para o frontend exibir
        Map<String, Object> pixData = asaasClient.createPixCharge(asaasId, price);
        
        return ResponseEntity.ok(Map.of(
            "paymentUrl", pixData.get("encodedImage"), // Imagem QR Code
            "payload", pixData.get("payload"),         // Copia e Cola
            "value", price
        ));
    }
}