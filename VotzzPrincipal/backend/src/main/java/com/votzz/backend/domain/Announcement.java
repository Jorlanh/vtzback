package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime; // Importante
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "announcements")
@EqualsAndHashCode(callSuper = true)
public class Announcement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @JsonIgnore
    private Tenant tenant;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String priority; 
    private String targetType; 
    private String targetValue; 

    private Boolean requiresConfirmation;

    // === NOVOS CAMPOS (Obrigatórios para o Service funcionar) ===

    private LocalDateTime autoArchiveDate; 
    private Boolean isArchived = false;

    @ElementCollection
    @CollectionTable(name = "announcement_reads", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "user_id")
    private Set<UUID> readBy = new HashSet<>();

    // Esse é o campo que estava faltando e gerando o erro:
    @Transient
    private boolean isReadByCurrentUser;
}