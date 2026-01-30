package com.votzz.backend.repository;

import com.votzz.backend.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    
    long countByAtivoTrue();

    List<Tenant> findByAtivoTrue();

    // [CORRIGIDO] Retorna Optional para evitar erro se não encontrar
    Optional<Tenant> findByAsaasCustomerId(String asaasCustomerId);

    // Método para calcular MRR no Dashboard
    List<Tenant> findByStatusAssinatura(String statusAssinatura);

    // Validações de unicidade no cadastro manual
    Optional<Tenant> findByCnpj(String cnpj);

    // Busca flexível por CNPJ ou Nome (Case insensitive)
    @Query("SELECT t FROM Tenant t WHERE t.cnpj = :identifier OR LOWER(t.nome) = LOWER(:identifier)")
    Optional<Tenant> findByCnpjOrNome(@Param("identifier") String identifier);
}