package com.votzz.backend.dto;

import java.time.LocalDate;

public record BookingRequest(
    String areaId,
    String userId,
    String unit,
    LocalDate date,
    String startTime,
    String endTime,
    
    // --- NOVOS CAMPOS PARA PAGAMENTO ---
    String billingType, // "PIX", "BOLETO", "CREDIT_CARD", "DEBIT_CARD"
    CreditCardDTO creditCard
) {
    public record CreditCardDTO(
        String holderName,
        String number,
        String expiryMonth,
        String expiryYear,
        String ccv
    ) {}
}