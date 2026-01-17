package com.votzz.backend.repository;

import com.votzz.backend.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    // O nome do método deve corresponder exatamente ao campo 'nextBillingDate' da entidade
    Optional<Subscription> findFirstByTenantIdOrderByNextBillingDateDesc(UUID tenantId);
}