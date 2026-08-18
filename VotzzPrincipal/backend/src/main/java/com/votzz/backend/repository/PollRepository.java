package com.votzz.backend.repository;

import com.votzz.backend.domain.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PollRepository extends JpaRepository<Poll, UUID> {
    List<Poll> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}