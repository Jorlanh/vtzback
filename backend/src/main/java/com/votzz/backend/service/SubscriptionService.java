package com.votzz.backend.service;

import com.votzz.backend.core.tenant.TenantContext;
import com.votzz.backend.domain.Subscription;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.repository.SubscriptionRepository;
import com.votzz.backend.repository.TenantRepository;
import com.votzz.backend.integration.AsaasClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final AsaasClient asaasClient;

    @Value("${votzz.kiwify.essencial.trimestral}") private String linkEssencialTrim;
    @Value("${votzz.kiwify.essencial.anual}") private String linkEssencialAnual;
    @Value("${votzz.kiwify.business.trimestral}") private String linkBusinessTrim;
    @Value("${votzz.kiwify.business.anual}") private String linkBusinessAnual;

    /**
     * Verifica se o condomínio precisa renovar (Faltando 60 dias ou menos)
     */
    public Map<String, Object> checkRenewalStatus(Tenant tenant) {
        LocalDate expiration = tenant.getDataExpiracaoPlano();
        Map<String, Object> response = new HashMap<>();
        
        response.put("currentPlan", tenant.getPlano().getNome());
        response.put("units", tenant.getUnidadesTotal());
        response.put("expirationDate", expiration);

        if (expiration == null) {
            response.put("needsRenewal", true);
            response.put("daysRemaining", 0);
            return response;
        }

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expiration);
        
        // REGRA: Aparece se faltar 60 dias ou menos
        boolean needsRenewal = daysRemaining <= 60;

        response.put("needsRenewal", needsRenewal);
        response.put("daysRemaining", daysRemaining);
        
        return response;
    }

    /**
     * Gera o Checkout de Renovação ou Novo Cadastro
     */
    public Map<String, Object> createCheckout(String planType, String cycle, int units, Tenant tenant, String userEmail, String mobilePhone) {
        String planName = planType.toLowerCase();
        
        // 1. LÓGICA KIWIFY (Planos Essencial e Business)
        if (planName.contains("essencial") || planName.contains("business")) {
            String baseUrl = "";
            if (planName.contains("essencial")) {
                baseUrl = "TRIMESTRAL".equalsIgnoreCase(cycle) ? linkEssencialTrim : linkEssencialAnual;
            } else {
                baseUrl = "TRIMESTRAL".equalsIgnoreCase(cycle) ? linkBusinessTrim : linkBusinessAnual;
            }
            
            String checkoutUrl = baseUrl + "?email=" + userEmail + "&name=" + tenant.getNome();
            return Map.of("redirectUrl", checkoutUrl, "gateway", "KIWIFY");
        }

        // 2. LÓGICA ASAAS (Plano Custom)
        BigDecimal price = calculatePrice(units, cycle);
        int maxInstallments = "TRIMESTRAL".equalsIgnoreCase(cycle) ? 3 : 12;

        try {
            String asaasId = tenant.getAsaasCustomerId();
            
            if (asaasId == null || asaasId.isBlank()) {
                asaasId = asaasClient.createCustomer(tenant.getNome(), tenant.getCnpj(), userEmail, mobilePhone);
                tenant.setAsaasCustomerId(asaasId);
                tenantRepository.save(tenant);
            }

            Map<String, Object> chargeData = asaasClient.createSubscriptionCharge(asaasId, price, maxInstallments);
            chargeData.put("calculatedValue", price);
            chargeData.put("redirectUrl", chargeData.get("invoiceUrl")); // Padroniza para o Front
            chargeData.put("gateway", "ASAAS");
            
            return chargeData;

        } catch (Exception e) {
            log.error("Erro checkout Asaas: {}", e.getMessage());
            throw new RuntimeException("Erro ao processar assinatura Custom: " + e.getMessage());
        }
    }

    public BigDecimal calculatePrice(int units, String cycle) {
        BigDecimal baseMensal = (units <= 30) ? new BigDecimal("190.00") : new BigDecimal("349.00");
        
        if (units > 80) {
            BigDecimal extras = new BigDecimal(units - 80);
            baseMensal = baseMensal.add(extras.multiply(new BigDecimal("1.50")));
        }

        BigDecimal finalPrice;
        if ("TRIMESTRAL".equalsIgnoreCase(cycle)) {
            finalPrice = baseMensal.multiply(new BigDecimal("3"));
        } else {
            finalPrice = baseMensal.multiply(new BigDecimal("12")).multiply(new BigDecimal("0.80"));
        }

        return finalPrice.setScale(2, RoundingMode.HALF_EVEN);
    }

    public Subscription getMySubscription() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return tenantId == null ? null : subscriptionRepository.findFirstByTenantIdOrderByNextBillingDateDesc(tenantId).orElse(null);
    }

    @Transactional
    public void renewSubscription(Tenant tenant, int monthsToAdd) {
        LocalDate today = LocalDate.now();
        LocalDate currentExp = tenant.getDataExpiracaoPlano();
        LocalDate newDate = (currentExp == null || currentExp.isBefore(today)) ? today.plusMonths(monthsToAdd) : currentExp.plusMonths(monthsToAdd);

        tenant.setDataExpiracaoPlano(newDate);
        tenantRepository.save(tenant);
    }
}