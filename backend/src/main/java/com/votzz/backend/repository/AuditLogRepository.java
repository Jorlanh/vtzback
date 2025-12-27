package com.votzz.backend.repository;

import com.votzz.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    // CORREÇÃO CRÍTICA:
    // O nome do método TEM que bater com o nome do campo na classe AuditLog.
    // Antes estava: ...OrderByTimestampDesc (Errado)
    // Agora está:   ...OrderByCreatedAtDesc (Certo)
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}