package com.votzz.backend.controller;

import com.votzz.backend.domain.Assembly;
import com.votzz.backend.domain.ChatMessage;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.ChatMessageRepository;
import com.votzz.backend.service.ChatService; // Importado
import com.votzz.backend.service.GeminiService; // Importado
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GeminiService geminiService; // Injetado para o chat da IA
    private final ChatService chatService;     // Injetado para o resumo em PDF

    // --- Endpoint para o Chat da S.I.R.I.U.S. (IA) ---
    @PostMapping("/api/chat/ask-ai")
    @ResponseBody
    public ResponseEntity<Map<String, String>> askAi(@RequestBody Map<String, Object> payload) {
        String message = (String) payload.get("message");
        boolean isLoggedIn = (boolean) payload.getOrDefault("isLoggedIn", false);
        
        String role = isLoggedIn ? "Assistente de Elite" : "Consultor Votzz";
        String response = geminiService.getAiResponse(message, role);
        
        return ResponseEntity.ok(Map.of("response", response));
    }

    // --- Endpoint para Baixar Resumo da Assembleia em PDF ---
    @GetMapping("/api/chat/assemblies/{assemblyId}/summary")
    @ResponseBody
    public ResponseEntity<byte[]> getSummary(@PathVariable UUID assemblyId) {
        byte[] pdf = chatService.gerarResumoAssembleia(assemblyId);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resumo_" + assemblyId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/api/chat/assemblies/{assemblyId}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<List<ChatMessageDTO>> getHistory(@PathVariable UUID assemblyId) {
        List<ChatMessage> messages = chatMessageRepository.findByAssemblyIdOrderByCreatedAtAsc(assemblyId);
        
        List<ChatMessageDTO> dtos = messages.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(dtos);
    }

    @MessageMapping("/chat/{assemblyId}/send")
    @Transactional
    public void sendMessage(@DestinationVariable UUID assemblyId, @Payload ChatMessageDTO messageDTO) {
        try {
            messageDTO.setTimestamp(LocalDateTime.now());
            messageDTO.setAssemblyId(assemblyId);

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

            messagingTemplate.convertAndSend("/topic/assembly/" + assemblyId, messageDTO);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ChatMessageDTO convertToDTO(ChatMessage entity) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setSenderName(entity.getSenderName());
        dto.setContent(entity.getContent());
        dto.setTimestamp(entity.getCreatedAt());
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