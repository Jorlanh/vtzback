package com.votzz.backend.repository;

import com.votzz.backend.domain.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {}