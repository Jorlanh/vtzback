package com.votzz.backend.repository;

import com.votzz.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    // Busca logs filtrando pelo ID do condomínio (Mais seguro e simples)
    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    // Busca todos os logs (Para o Admin Global)
    List<AuditLog> findAllByOrderByCreatedAtDesc();

    // Adicione para buscar logs por período (para provar presença/acessos)
    List<AuditLog> findByTenantIdAndCreatedAtBetween(UUID tenantId, LocalDateTime start, LocalDateTime end);

}