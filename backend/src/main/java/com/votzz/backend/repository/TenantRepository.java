package com.votzz.backend.repository;

import com.votzz.backend.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    
    // Conta quantos condomínios estão ativos
    long countByAtivoTrue();

    // Busca condomínios ativos para cálculo de MRR
    List<Tenant> findByAtivoTrue();

    // Novo método para o Webhook (Necessário para identificar quem pagou)
    Tenant findByAsaasCustomerId(String asaasCustomerId);
}