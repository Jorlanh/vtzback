package com.votzz.backend.repository;

import com.votzz.backend.domain.CommonArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommonAreaRepository extends JpaRepository<CommonArea, UUID> {
    
    // CORREÇÃO: Ensinamos ao Spring que 'tenantId' se refere a 'tenant.id'
    @Query("SELECT c FROM CommonArea c WHERE c.tenant.id = :tenantId")
    List<CommonArea> findByTenantId(@Param("tenantId") UUID tenantId);
}