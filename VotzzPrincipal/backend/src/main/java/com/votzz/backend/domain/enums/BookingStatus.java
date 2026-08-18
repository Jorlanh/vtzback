package com.votzz.backend.domain.enums;

public enum BookingStatus {
    PENDING,        // Aguardando pagamento
    APPROVED,       // Aprovado (Sinônimo de CONFIRMED)
    CONFIRMED,      // Confirmado (Pago)
    REJECTED,       // Rejeitado pelo síndico
    CANCELLED,      // Cancelado pelo morador
    COMPLETED,      // Realizado
    UNDER_ANALYSIS, // Aguardando validação do comprovante (NOVO)
    EXPIRED         // Expirou o tempo de pagamento (NOVO)
}