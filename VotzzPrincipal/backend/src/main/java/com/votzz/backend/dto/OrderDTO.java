package com.votzz.backend.dto;

import com.votzz.backend.domain.Order;
import com.votzz.backend.domain.enums.OrderStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OrderDTO {
    private UUID id;
    private String trackingCode;
    private String origin;
    private String recipientName;
    private LocalDateTime arrivalDate;
    private OrderStatus status;

    // Dados Residente
    private UUID residentId;
    private String residentName;
    private String unit;
    private String block;
    private String residentEmail;
    private String residentCpf;
    private String residentWhatsapp;

    // Assinaturas
    private LocalDateTime residentSignatureDate;
    private String residentSignatureName;
    private LocalDateTime staffSignatureDate;
    private String staffSignatureName;

    // Construtor a partir da Entidade
    public OrderDTO(Order entity) {
        this.id = entity.getId();
        this.trackingCode = entity.getTrackingCode();
        this.origin = entity.getOrigin();
        this.recipientName = entity.getRecipientName();
        this.arrivalDate = entity.getArrivalDate();
        this.status = entity.getStatus();
        
        this.residentId = entity.getResidentId();
        this.residentName = entity.getResidentName();
        this.unit = entity.getUnit();
        this.block = entity.getBlock();
        this.residentEmail = entity.getResidentEmail();
        this.residentCpf = entity.getResidentCpf();
        this.residentWhatsapp = entity.getResidentWhatsapp();

        this.residentSignatureDate = entity.getResidentSignatureDate();
        this.residentSignatureName = entity.getResidentSignatureName();
        this.staffSignatureDate = entity.getStaffSignatureDate();
        this.staffSignatureName = entity.getStaffSignatureName();
    }
}