package com.votzz.backend.controller;

import com.votzz.backend.domain.Assembly;
import com.votzz.backend.domain.ChatMessage;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.ChatMessageRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // --- CORREÇÃO: Endpoint REST retorna DTO para evitar Loop Infinito/Erro 400 ---
    @GetMapping("/api/chat/assemblies/{assemblyId}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<List<ChatMessageDTO>> getHistory(@PathVariable UUID assemblyId) {
        List<ChatMessage> messages = chatMessageRepository.findByAssemblyIdOrderByCreatedAtAsc(assemblyId);
        
        // Converte a lista de entidades para DTOs simples
        List<ChatMessageDTO> dtos = messages.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(dtos);
    }

    // --- WebSocket para mensagens em tempo real ---
    @MessageMapping("/chat/{assemblyId}/send")
    @Transactional
    public void sendMessage(@DestinationVariable UUID assemblyId, @Payload ChatMessageDTO messageDTO) {
        try {
            messageDTO.setTimestamp(LocalDateTime.now());
            messageDTO.setAssemblyId(assemblyId);

            // Salva no Banco
            ChatMessage entity = new ChatMessage();
            
            Assembly asm = new Assembly();
            asm.setId(assemblyId);
            entity.setAssembly(asm);

            if (messageDTO.getUserId() != null) {
                User u = new User();
                u.setId(messageDTO.getUserId());
                entity.setUser(u);
            }
            if (messageDTO.getTenantId() != null) {
                Tenant t = new Tenant();
                t.setId(messageDTO.getTenantId());
                entity.setTenant(t);
            }

            entity.setSenderName(messageDTO.getSenderName());
            entity.setContent(messageDTO.getContent());
            entity.setType("CHAT");
            
            chatMessageRepository.save(entity);

            // Envia para o Front via Socket
            messagingTemplate.convertAndSend("/topic/assembly/" + assemblyId, messageDTO);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Converter Entidade -> DTO
    private ChatMessageDTO convertToDTO(ChatMessage entity) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setSenderName(entity.getSenderName());
        dto.setContent(entity.getContent());
        dto.setTimestamp(entity.getCreatedAt()); // Pega a data base
        dto.setType(entity.getType());
        dto.setAssemblyId(entity.getAssembly().getId());
        if (entity.getUser() != null) dto.setUserId(entity.getUser().getId());
        if (entity.getTenant() != null) dto.setTenantId(entity.getTenant().getId());
        return dto;
    }

    @Data
    public static class ChatMessageDTO {
        private String senderName;
        private String content;
        private LocalDateTime timestamp;
        private UUID assemblyId;
        private UUID userId;    
        private UUID tenantId; 
        private String type;
    }
}