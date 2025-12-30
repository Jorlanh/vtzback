package com.votzz.backend.controller;

import com.votzz.backend.domain.Assembly;
import com.votzz.backend.domain.ChatMessage;
import com.votzz.backend.domain.Tenant; // [IMPORTANTE] Importar Tenant
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
        
        // --- MAPEAMENTO DE IDs PARA OBJETOS (Hibernate Proxy) ---
        
        // Assembly
        Assembly assemblyRef = new Assembly();
        assemblyRef.setId(assemblyId);
        entity.setAssembly(assemblyRef); 

        // User
        User userRef = new User();
        userRef.setId(messageDTO.getUserId());
        entity.setUser(userRef); 

        // --- CORREÇÃO DO ERRO AQUI ---
        // Em vez de setTenantId, criamos um objeto Tenant com o ID
        if (messageDTO.getTenantId() != null) {
            Tenant tenantRef = new Tenant();
            tenantRef.setId(messageDTO.getTenantId());
            entity.setTenant(tenantRef); // Agora usamos setTenant!
        }

        // Campos simples
        entity.setUserName(messageDTO.getSenderName());
        entity.setContent(messageDTO.getContent());
        
        // O BaseEntity preenche o 'createdAt' automaticamente no banco.

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