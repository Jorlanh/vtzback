package com.votzz.backend.repository;

import com.votzz.backend.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    // Método necessário para verificar duplicidade
    boolean existsByAssemblyIdAndUserId(UUID assemblyId, UUID userId);

    // Método necessário para o RELATÓRIO CSV
    List<Vote> findByAssemblyId(UUID assemblyId);
}