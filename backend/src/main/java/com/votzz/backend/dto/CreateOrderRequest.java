package com.votzz.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    private String trackingCode;
    private String origin;
    private String recipientName;
    private LocalDateTime arrivalDate;

    // ID do morador selecionado na busca
    private UUID residentId;
    // Opcional: Se quiser confiar no frontend enviando os dados, ou o backend busca pelo ID
    // Vou assumir que o frontend envia o snapshot para flexibilidade
    private String residentName;
    private String unit;
    private String block;
    private String residentEmail;
    private String residentCpf;
    private String residentWhatsapp;
}