package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_messages")
@Data
@EqualsAndHashCode(of = "id")
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    @JsonBackReference // Evita loop infinito no JSON
    private Ticket ticket;

    @Column(name = "sender_id")
    private UUID senderId;

    private String senderName;
    
    // Flag para saber se quem mandou foi Admin (ajuda a colorir o chat no front)
    private boolean isAdminSender; 

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}