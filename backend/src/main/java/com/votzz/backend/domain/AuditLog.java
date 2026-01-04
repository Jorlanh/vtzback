package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore; // <--- IMPORT ESSENCIAL
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_logs")
@EqualsAndHashCode(of = "id")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String timestamp;    
    private String action;
    
    @Column(name = "user_id")
    private String userId;       
    
    @Column(name = "user_name")
    private String userName;     
    
    // --- CORREÇÃO DEFINITIVA AQUI ---
    // O @JsonIgnore impede que o Jackson tente ler o Proxy do Hibernate
    // Isso ELIMINA o erro "ByteBuddyInterceptor" e o 400 Bad Request.
    @JsonIgnore 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(columnDefinition = "TEXT") 
    private String details;      
    
    @Column(name = "resource_type")
    private String resourceType; 

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}