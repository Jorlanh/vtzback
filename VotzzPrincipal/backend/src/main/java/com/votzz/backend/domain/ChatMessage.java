package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "chat_message")
@EqualsAndHashCode(callSuper = true)
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembly_id", nullable = false)
    private Assembly assembly;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "sender_name") // Mapeia para sender_name no banco
    private String senderName;

    // Campo auxiliar para compatibilidade com DTOs antigos que usam userName
    @Transient
    public String getUserName() {
        return senderName;
    }

    public void setUserName(String userName) {
        this.senderName = userName;
    }

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String type; // CHAT, STATUS_UPDATE
}