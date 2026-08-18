package com.votzz.backend.domain;

import com.votzz.backend.domain.enums.GuestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_guests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "guest_name", nullable = false)
    private String guestName;

    @Column(name = "guest_rg", nullable = false)
    private String guestRg;

    // Novo campo: Data agendada para a visita
    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "access_code", nullable = false, unique = true)
    private String accessCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuestStatus status;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "resident_name")
    private String residentName;

    private String unit;
    private String block;

    @Column(name = "resident_whatsapp")
    private String residentWhatsapp;

    // Novo campo: CPF do morador responsável
    @Column(name = "resident_cpf")
    private String residentCpf;

    // Novo campo: E-mail do morador responsável
    @Column(name = "resident_email")
    private String residentEmail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}