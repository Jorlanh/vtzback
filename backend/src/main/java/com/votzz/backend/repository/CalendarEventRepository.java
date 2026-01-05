package com.votzz.backend.repository;

import com.votzz.backend.domain.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
    List<CalendarEvent> findByTenantId(UUID tenantId);
}