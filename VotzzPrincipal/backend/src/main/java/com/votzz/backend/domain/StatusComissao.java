package com.votzz.backend.domain;

public enum StatusComissao {
    BLOQUEADO,   // Aguardando prazo de garantia (30 dias)
    DISPONIVEL,  // Liberado para saque/pagamento
    PAGO,        // Transferência realizada
    CANCELADO    // Assinatura cancelada/reembolso
}