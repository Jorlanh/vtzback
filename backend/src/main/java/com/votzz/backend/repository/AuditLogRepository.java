package com.votzz.backend.repository;

import com.votzz.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    // Usado por Síndicos
    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    // Usado por Admin Global
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}