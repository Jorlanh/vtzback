package com.votzz.backend.service;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TenantRepository tenantRepository;

    /**
     * Cálculo Server-Side (Anti-Hacker): 
     * Recalcula o valor do plano customizado ignorando inputs de preço do front.
     */
    public BigDecimal calculateCustomPlanPrice(int units, String frequency) {
        if (units <= 80) return new BigDecimal("490.00"); // Base Business
        
        BigDecimal baseValue = new BigDecimal("490.00");
        BigDecimal perUnitValue = new BigDecimal("2.50");
        BigDecimal totalMonthly = baseValue.add(perUnitValue.multiply(new BigDecimal(units - 80)));

        if ("ANUAL".equalsIgnoreCase(frequency)) {
            // Aplica 20% de desconto conforme regra do Pricing.tsx
            return totalMonthly.multiply(new BigDecimal("12")).multiply(new BigDecimal("0.8"));
        }
        return totalMonthly.multiply(new BigDecimal("3")); // Trimestral padrão
    }

    public void processSubscriptionActivation(Tenant tenant, int months) {
        LocalDate today = LocalDate.now();
        LocalDate currentExpiration = tenant.getDataExpiracaoPlano();

        LocalDate newDate = (currentExpiration == null || currentExpiration.isBefore(today))
                ? today.plusMonths(months)
                : currentExpiration.plusMonths(months);

        tenant.setDataExpiracaoPlano(newDate);
        tenant.setStatusAssinatura("ACTIVE");
        tenant.setAtivo(true);
        tenantRepository.save(tenant);
    }
}