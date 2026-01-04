package com.votzz.backend.repository;

import com.votzz.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    // Busca logs filtrando pelo ID do condomínio (Mais seguro e simples)
    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    // Busca todos os logs (Para o Admin Global)
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}