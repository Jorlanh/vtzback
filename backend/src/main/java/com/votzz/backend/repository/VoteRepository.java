package com.votzz.backend.repository;

import com.votzz.backend.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {
    boolean existsByAssemblyIdAndUserId(UUID assemblyId, UUID userId);

    // FIX para o ReportService
    List<Vote> findByAssemblyId(UUID assemblyId);

    // FIX para o CondoDashboardService
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndCreatedAtAfter(UUID tenantId, LocalDateTime startDate);
}