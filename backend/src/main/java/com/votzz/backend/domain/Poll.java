package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
// IMPORTANTE: Força o plural 'polls'
@Table(name = "polls")
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    private String status; // OPEN, CLOSED

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_by")
    private UUID createdBy;
}