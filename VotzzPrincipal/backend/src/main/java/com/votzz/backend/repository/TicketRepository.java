package com.votzz.backend.repository;

import com.votzz.backend.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId);
}