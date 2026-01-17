package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data // Gera getters, setters, toString, equals, hashCode automaticamente
@Entity
@Table(name = "condo_financial")
public class CondoFinancial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // --- CORREÇÃO: Adicionando o relacionamento com Tenant ---
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id")
    @JsonIgnore // Importante para evitar loop infinito no JSON
    private Tenant tenant;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}