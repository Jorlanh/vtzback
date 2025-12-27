package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.UUID;

@Entity
@Table(name = "tickets") // Força o Hibernate a procurar por 'tickets' em vez de 'ticket'
@Data
@EqualsAndHashCode(callSuper = true)
public class Ticket extends BaseEntity {
    private String title;
    private String description;
    private String status; 
    private String priority; 
    private UUID userId;
}