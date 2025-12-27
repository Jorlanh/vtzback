package com.votzz.backend.controller;

import com.votzz.backend.domain.Assembly;
import com.votzz.backend.domain.ChatMessage;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.ChatMessageRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chat/{assemblyId}/send")
    @SendTo("/topic/assembly/{assemblyId}")
    public ChatMessageDTO sendMessage(@DestinationVariable UUID assemblyId, ChatMessageDTO messageDTO) {
        
        // 1. Configurar o timestamp NO DTO (para quem recebe ver a hora)
        messageDTO.setTimestamp(LocalDateTime.now());
        messageDTO.setAssemblyId(assemblyId);

        // 2. SALVAR NO BANCO
        ChatMessage entity = new ChatMessage();
        
        // --- CORREÇÃO 1: Mapear IDs para Objetos (Hibernate Proxy) ---
        // Não precisamos buscar no banco, basta criar um objeto com o ID setado.
        
        Assembly assemblyRef = new Assembly();
        assemblyRef.setId(assemblyId);
        entity.setAssembly(assemblyRef); // Usa setAssembly, não setAssemblyId

        User userRef = new User();
        userRef.setId(messageDTO.getUserId());
        entity.setUser(userRef); // Usa setUser, não setUserId

        // --- CORREÇÃO 2: Campos simples ---
        entity.setUserName(messageDTO.getSenderName());
        entity.setContent(messageDTO.getContent());
        entity.setTenantId(messageDTO.getTenantId()); 

        // --- CORREÇÃO 3: Timestamp ---
        // Não usamos entity.setTimestamp(). 
        // A BaseEntity preenche 'createdAt' automaticamente ao salvar.

        chatMessageRepository.save(entity);

        return messageDTO;
    }

    @Data
    public static class ChatMessageDTO {
        private String senderName;
        private String content;
        private LocalDateTime timestamp;
        private UUID assemblyId;
        private UUID userId;   
        private UUID tenantId; 
    }
}