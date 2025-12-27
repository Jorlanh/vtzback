package com.votzz.backend.repository;

import com.votzz.backend.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    // --- CORREÇÃO AQUI ---
    // De: OrderByTimestampAsc
    // Para: OrderByCreatedAtAsc
    List<ChatMessage> findByAssemblyIdOrderByCreatedAtAsc(UUID assemblyId);
}