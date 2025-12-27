package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "chat_message")
@EqualsAndHashCode(callSuper = true)
public class ChatMessage extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "assembly_id")
    private Assembly assembly;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_name")
    private String userName;

    private String content;

    // REMOVIDO: private LocalDateTime timestamp;
    // O horário da mensagem é o 'createdAt' herdado da BaseEntity
}