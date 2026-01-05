package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore; // Importante!
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "calendar_events")
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @JsonIgnore // <--- ADICIONE ISSO
    private Tenant tenant;

    private String title;
    private String type; // SOCIAL, MAINTENANCE, MEETING
    private LocalDate date;
}