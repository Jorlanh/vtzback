package com.votzz.backend.repository;

import com.votzz.backend.domain.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID; // <--- Faltava esse import

@Repository
//                                                            ↓↓ Mudei de String para UUID
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    
    // Dica: Como seu sistema é multi-tenant, você provavelmente vai precisar disso em breve:
    // List<Announcement> findByTenantId(UUID tenantId);
}