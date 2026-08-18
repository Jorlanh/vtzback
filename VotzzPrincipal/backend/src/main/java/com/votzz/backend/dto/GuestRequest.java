package com.votzz.backend.dto;

import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class GuestRequest {
    private String guestName;
    private String guestRg;
    private LocalDateTime scheduledDate; // Adicionado
    private UUID residentId;
    private String residentName;
    private String unit;
    private String block;
    private String residentWhatsapp;
    private String residentCpf; // Adicionado
    private String residentEmail; // Adicionado
}