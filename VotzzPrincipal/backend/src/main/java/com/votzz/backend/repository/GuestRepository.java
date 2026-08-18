package com.votzz.backend.repository;

import com.votzz.backend.domain.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, UUID> {
    
    // Busca TODOS do condomínio (Para a Gestão e Portaria)
    List<Guest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    // Busca apenas os do Morador específico (Visão isolada do morador)
    List<Guest> findByTenantIdAndResidentIdOrderByCreatedAtDesc(UUID tenantId, UUID residentId);
    
    Optional<Guest> findByAccessCodeAndTenantId(String accessCode, UUID tenantId);
}