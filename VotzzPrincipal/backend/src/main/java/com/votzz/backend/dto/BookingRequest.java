package com.votzz.backend.dto;

public record BookingRequest(
    String areaId,
    String userId,
    String date,       // Recebe como String (ex: "2026-02-10")
    String startTime,  // Recebe como String (ex: "10:00")
    String endTime,
    
    // Campos do Morador (Snapshot)
    String unit,       // Unidade/Apt
    String block,      // Bloco
    String nome,       // Nome Responsável
    String cpf,
    String whatsapp,
    
    String billingType // "PIX" ou "FREE"
) {}