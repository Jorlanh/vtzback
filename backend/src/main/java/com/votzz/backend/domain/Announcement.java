package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "announcements")
@EqualsAndHashCode(callSuper = true)
public class Announcement extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String title;

    private String content;

    private String priority; // NORMAL, HIGH

    @Column(name = "target_type")
    private String targetType; // ALL, OWNER, TENANT

    @Column(name = "requires_confirmation")
    private Boolean requiresConfirmation;

    // REMOVIDO: private LocalDateTime date;
    // A data do comunicado agora é o 'createdAt' herdado da BaseEntity
}