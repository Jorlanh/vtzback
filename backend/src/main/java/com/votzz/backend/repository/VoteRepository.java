package com.votzz.backend.repository;

import com.votzz.backend.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {
    // MÉTODO ANTIGO (Pode manter para retrocompatibilidade)
    boolean existsByAssemblyIdAndUserId(UUID assemblyId, UUID userId);

    // --- NOVO MÉTODO CRÍTICO ---
    // Verifica se uma unidade específica (texto) já votou nesta assembleia
    boolean existsByAssemblyIdAndUnidade(UUID assemblyId, String unidade);

    List<Vote> findByAssemblyId(UUID assemblyId);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndCreatedAtAfter(UUID tenantId, LocalDateTime startDate);
}