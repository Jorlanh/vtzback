package com.votzz.backend.service;

import com.votzz.backend.domain.AuditLog;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(User actor, Tenant targetTenant, String action, String details, String resourceType) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(LocalDateTime.now().toString());
            log.setAction(action);
            log.setDetails(details);
            log.setResourceType(resourceType);
            
            if (actor != null) {
                log.setUserId(actor.getId().toString());
                String roleTag = actor.getRole() != null ? " [" + actor.getRole() + "]" : "";
                log.setUserName(actor.getNome() + roleTag);
                
                // Lógica de Prioridade de Tenant:
                // 1. Se quem fez a ação tem tenant (morador/síndico), usa o dele.
                // 2. Se não tem (Admin Votzz), usa o tenant ALVO da ação.
                if (actor.getTenant() != null) {
                    log.setTenant(actor.getTenant());
                } else if (targetTenant != null) {
                    log.setTenant(targetTenant);
                }
            } else {
                log.setUserId("SYSTEM");
                log.setUserName("Sistema Votzz");
                if (targetTenant != null) log.setTenant(targetTenant);
            }

            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Erro na auditoria: " + e.getMessage());
        }
    }
}