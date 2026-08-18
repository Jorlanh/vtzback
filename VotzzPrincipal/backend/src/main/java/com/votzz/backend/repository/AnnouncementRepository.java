package com.votzz.backend.repository;

import com.votzz.backend.domain.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    // Importante: OrderByCreatedAtDesc garante a ordem na linha do tempo
    List<Announcement> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}