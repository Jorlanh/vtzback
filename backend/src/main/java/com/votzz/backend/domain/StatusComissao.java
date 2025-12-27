package com.votzz.backend.domain;

public enum StatusComissao {
    BLOQUEADO,  // Aguardando D+30
    DISPONIVEL, // Liberado para saque
    PAGO        // Transferido via PIX
}