package com.votzz.backend.repository;

import com.votzz.backend.domain.TenantPaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TenantPaymentConfigRepository extends JpaRepository<TenantPaymentConfig, UUID> {
    Optional<TenantPaymentConfig> findByTenantId(UUID tenantId);
}