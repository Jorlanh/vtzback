package com.votzz.backend.domain;

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
    
    // --- ESTE É O CAMPO QUE ESTÁ FALTANDO ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    // ----------------------------------------

    @Column(columnDefinition = "TEXT") 
    private String details;      
    
    @Column(name = "resource_type")
    private String resourceType; 

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}