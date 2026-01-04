package com.votzz.backend.repository;

import com.votzz.backend.domain.FinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, UUID> {
    // Busca relatórios do condomínio ordenados do mais recente para o mais antigo
    List<FinancialReport> findByTenantIdOrderByYearDescCreatedAtDesc(UUID tenantId);
}