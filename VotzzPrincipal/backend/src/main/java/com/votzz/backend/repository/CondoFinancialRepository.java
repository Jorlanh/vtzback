package com.votzz.backend.repository;

import com.votzz.backend.domain.CondoFinancial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CondoFinancialRepository extends JpaRepository<CondoFinancial, UUID> {
    // Método correto para isolamento de dados
    Optional<CondoFinancial> findByTenantId(UUID tenantId);
    
    // Remova ou evite usar este se ele pegava o último global:
    // CondoFinancial findFirstByOrderByLastUpdateDesc(); 
}