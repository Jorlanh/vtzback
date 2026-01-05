package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore; // Importante!
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
    @JsonIgnore // <--- ADICIONE ISSO: Remove o 'tenant' do JSON enviado ao front
    private Tenant tenant;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String priority; // NORMAL, HIGH
    private String targetType; 
    private String targetValue; 

    private Boolean requiresConfirmation;

    @ElementCollection
    @CollectionTable(name = "announcement_reads", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "user_id")
    private Set<UUID> readBy = new HashSet<>();
}