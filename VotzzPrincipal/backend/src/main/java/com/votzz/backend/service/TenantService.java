package com.votzz.backend.service;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        // 1. Validação de Unicidade por CNPJ para bloquear múltiplos testes grátis
        if (tenant.getCnpj() != null && !tenant.getCnpj().isBlank()) {
            // Remove pontuação para busca limpa se necessário, ou busca exata
            String cleanCnpj = tenant.getCnpj().replaceAll("\\D", "");
            
            // Busca considerando que no banco pode estar limpo ou formatado (depende da sua padronização)
            // Aqui buscamos exatamente como veio
            Optional<Tenant> existingTenant = tenantRepository.findByCnpj(tenant.getCnpj());
            
            if (existingTenant.isPresent()) {
                throw new IllegalArgumentException("Este CNPJ já está cadastrado. O período de teste grátis é válido apenas uma vez por condomínio.");
            }
        }

        // 2. Configuração do Plano Free (30 Dias)
        tenant.setAtivo(true);
        tenant.setStatusAssinatura("TRIAL"); // Status de Teste
        tenant.setDataExpiracaoPlano(LocalDate.now().plusDays(30)); // 30 dias grátis a partir de hoje

        return tenantRepository.save(tenant);
    }
    
    public Tenant updateTenant(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    public void activateSubscription(Tenant tenant, int months) {
        tenant.renovarAssinatura(months);
        tenantRepository.save(tenant);
    }
}