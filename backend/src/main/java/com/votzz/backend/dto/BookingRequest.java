package com.votzz.backend.dto;

import java.time.LocalDate;

public record BookingRequest(
    String areaId,
    String userId,
    String unit,
    LocalDate date,
    String startTime,
    String endTime,
    
    // --- PAGAMENTO (Apenas PIX ou BOLETO agora) ---
    String billingType // Esperado: "PIX" ou "BOLETO"
) {
    // CreditCardDTO removido pois não processamos mais cartão
}