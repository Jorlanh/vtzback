package com.votzz.backend.dto;

import com.votzz.backend.domain.ChatMessage;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageDTO(
    UUID id, 
    String content, 
    String userName, 
    LocalDateTime timestamp // Pode manter o nome no JSON
) {
    public static ChatMessageDTO fromEntity(ChatMessage entity) {
        return new ChatMessageDTO(
            entity.getId(),
            entity.getContent(),
            entity.getUserName(),
            entity.getCreatedAt() // Lê do campo herdado
        );
    }
}