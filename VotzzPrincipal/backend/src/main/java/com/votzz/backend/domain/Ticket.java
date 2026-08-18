package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.votzz.backend.domain.enums.TicketPriority;
import com.votzz.backend.domain.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Data
@EqualsAndHashCode(of = "id")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    
    @Column(name = "user_id")
    private UUID userId;
    
    // Snapshot dos dados do usuário
    private String userName;
    private String userUnit;
    private String userBlock;
    
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority;
    
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Permite serializar as mensagens ao buscar o ticket
    private List<TicketMessage> messages = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = TicketStatus.OPEN;
        // Prioridade padrão é BAIXA até o Admin definir
        if (priority == null) priority = TicketPriority.LOW; 
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}