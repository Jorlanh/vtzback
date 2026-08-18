package com.votzz.backend.repository;

import com.votzz.backend.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    // Lista todas as encomendas do condomínio (ordem de chegada mais recente)
    List<Order> findByTenantIdOrderByArrivalDateDesc(UUID tenantId);

    // Lista apenas as encomendas de um morador específico
    List<Order> findByTenantIdAndResidentIdOrderByArrivalDateDesc(UUID tenantId, UUID residentId);
}