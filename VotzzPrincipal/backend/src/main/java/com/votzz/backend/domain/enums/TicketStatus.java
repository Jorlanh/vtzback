package com.votzz.backend.domain.enums;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_TENANT, // Aguardando resposta do morador
    RESOLVED,
    CLOSED
}