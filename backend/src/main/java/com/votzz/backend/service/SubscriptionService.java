package com.votzz.backend.service;

import com.votzz.backend.core.tenant.TenantContext;
import com.votzz.backend.domain.Subscription;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.repository.SubscriptionRepository;
import com.votzz.backend.repository.TenantRepository;
import com.votzz.backend.integration.AsaasClient; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TenantRepository tenantRepository;
    
    @Autowired
    private AsaasClient asaasClient;

    /**
     * Gera o checkout (PIX) no Asaas
     */
    public Map<String, Object> createCheckout(String planType, String cycle, int units, Tenant tenant, String userEmail) {
        BigDecimal price = calculatePrice(units, cycle);

        // 1. Garante cliente no Asaas
        String asaasId = tenant.getAsaasCustomerId();
        if (asaasId == null) {
            asaasId = asaasClient.createCustomer(tenant.getNome(), tenant.getCnpj(), userEmail);
            tenant.setAsaasCustomerId(asaasId);
            tenantRepository.save(tenant);
        }

        // 2. Cria cobrança
        Map<String, Object> pixData = asaasClient.createPixCharge(asaasId, price);
        
        // Adiciona o valor calculado ao retorno para conferência
        pixData.put("value", price);
        
        return pixData;
    }

    /**
     * Lógica de Preço Especificada:
     * Business: Trimestral (1.047) | Anual (3.350,40)
     * Custom: Base do Business + (1,50 * unidades)
     */
    private BigDecimal calculatePrice(int units, String cycle) {
        BigDecimal baseTrimestral = new BigDecimal("1047.00");
        BigDecimal baseAnual = new BigDecimal("3350.40");
        
        BigDecimal finalPrice;

        // Se for Trimestral
        if ("TRIMESTRAL".equalsIgnoreCase(cycle)) {
            finalPrice = baseTrimestral;
        } else {
            // Anual
            finalPrice = baseAnual;
        }

        // Se for Custom (> 80 unidades), adiciona a taxa por unidade
        if (units > 80) {
            // Lógica: Preço Base + (1.50 * total de unidades)
            BigDecimal unitPrice = new BigDecimal("1.50");
            BigDecimal variablePart = unitPrice.multiply(new BigDecimal(units));
            
            finalPrice = finalPrice.add(variablePart);
        }
        
        return finalPrice.setScale(2, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Busca a assinatura do contexto atual
     */
    public Subscription getMySubscription() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) return null;
        
        // CORREÇÃO: Alterado de OrderByEndDateDesc para OrderByNextBillingDateDesc
        // Isso deve corresponder ao nome do método definido no SubscriptionRepository
        return subscriptionRepository.findFirstByTenantIdOrderByNextBillingDateDesc(tenantId).orElse(null);
    }

    /**
     * Renova a assinatura
     */
    @Transactional
    public void renewSubscription(Tenant tenant, int monthsToAdd) {
        LocalDate today = LocalDate.now();
        // Nota: Ajuste se o campo na entidade for 'nextBillingDate' e do tipo LocalDateTime
        // Aqui estou assumindo que você tem um método auxiliar ou que a entidade usa LocalDate.
        // Se a entidade usa LocalDateTime para nextBillingDate, use: tenant.getNextBillingDate().toLocalDate()
        // Abaixo segue lógica genérica baseada no seu código anterior:
        
        // Exemplo adaptado para LocalDateTime se necessário (verifique sua entidade Tenant/Subscription)
        // Se Tenant tem 'dataExpiracaoPlano' (LocalDate), mantenha assim:
        LocalDate currentExpiration = tenant.getDataExpiracaoPlano();
        LocalDate newExpirationDate;

        if (currentExpiration == null || currentExpiration.isBefore(today)) {
            newExpirationDate = today.plusMonths(monthsToAdd);
        } else {
            newExpirationDate = currentExpiration.plusMonths(monthsToAdd);
        }

        tenant.setDataExpiracaoPlano(newExpirationDate);
        // tenant.setAtivo(true); 
        tenantRepository.save(tenant);
    }
}