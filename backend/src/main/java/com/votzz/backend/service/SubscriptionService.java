package com.votzz.backend.service;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SubscriptionService {

    @Autowired
    private TenantRepository tenantRepository;

    @Transactional
    public void renewSubscription(Tenant tenant, int monthsToAdd) {
        LocalDate today = LocalDate.now();
        LocalDate currentExpiration = tenant.getDataExpiracaoPlano();

        LocalDate newExpirationDate;

        if (currentExpiration == null || currentExpiration.isBefore(today)) {
            // Caso 1: Plano já venceu ou é novo. 
            // A nova data começa a contar de HOJE.
            newExpirationDate = today.plusMonths(monthsToAdd);
        } else {
            // Caso 2: Plano ainda está ativo (Renovação antecipada).
            // A nova data soma os meses à DATA FINAL ATUAL (preserva os dias restantes).
            newExpirationDate = currentExpiration.plusMonths(monthsToAdd);
        }

        tenant.setDataExpiracaoPlano(newExpirationDate);
        tenant.setAtivo(true); // Garante que reativa se estivesse suspenso
        tenantRepository.save(tenant);
        
        System.out.println("Plano renovado para o condomínio: " + tenant.getNome());
        System.out.println("Nova data de vencimento: " + newExpirationDate);
    }
}