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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Endpoint REST para carregar histórico
    @GetMapping("/api/chat/assemblies/{assemblyId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable UUID assemblyId) {
        List<ChatMessage> messages = chatMessageRepository.findByAssemblyIdOrderByCreatedAtAsc(assemblyId);
        return ResponseEntity.ok(messages);
    }

    // WebSocket para mensagens em tempo real
    @MessageMapping("/chat/{assemblyId}/send")
    public void sendMessage(@DestinationVariable UUID assemblyId, @Payload ChatMessageDTO messageDTO) {
        try {
            messageDTO.setTimestamp(LocalDateTime.now());
            messageDTO.setAssemblyId(assemblyId);

            ChatMessage entity = new ChatMessage();
            
            Assembly assemblyRef = new Assembly();
            assemblyRef.setId(assemblyId);
            entity.setAssembly(assemblyRef); 

            if (messageDTO.getUserId() != null) {
                User userRef = new User();
                userRef.setId(messageDTO.getUserId());
                entity.setUser(userRef); 
            }

            if (messageDTO.getTenantId() != null) {
                Tenant tenantRef = new Tenant();
                tenantRef.setId(messageDTO.getTenantId());
                entity.setTenant(tenantRef); 
            }

            entity.setSenderName(messageDTO.getSenderName());
            entity.setContent(messageDTO.getContent());
            entity.setType("CHAT");
            
            chatMessageRepository.save(entity);
            messagingTemplate.convertAndSend("/topic/assembly/" + assemblyId, messageDTO);

        } catch (Exception e) {
            e.printStackTrace();
        }
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