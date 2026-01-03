package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Data/Hora da ação (String ISO-8601 é mais seguro para JSON)
    private String timestamp;    

    // Nome da ação (ex: CRIAR_USUARIO)
    private String action;
    
    @Column(name = "user_id")
    private String userId;       // String para evitar erro de cast com UUID
    
    @Column(name = "user_name")
    private String userName;     // Nome do admin para exibir na tabela
    
    @Column(columnDefinition = "TEXT") 
    private String details;      // Detalhes da ação em texto
    
    @Column(name = "resource_type")
    private String resourceType; // Ex: "ADMIN_PANEL"

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}