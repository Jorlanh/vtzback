package com.votzz.backend.repository;

import com.votzz.backend.domain.Assembly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssemblyRepository extends JpaRepository<Assembly, UUID> {
    List<Assembly> findByTenantId(UUID tenantId);
    
    // Conta assembleias ativas por condomínio
    long countByTenantIdAndStatus(UUID tenantId, String status);

    // Lista todas por status e condomínio
    List<Assembly> findByTenantIdAndStatus(UUID tenantId, String status);
}