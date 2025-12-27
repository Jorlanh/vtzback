package com.votzz.backend.domain;

public enum Role {
    ADMIN,       // Super Admin do SaaS (Dono da Votzz)
    SINDICO,     // Síndico (Gestor do Condomínio)
    ADM_CONDO,   // Administradora (Empresa terceira) - ADICIONADO
    MORADOR,     // Usuário comum (Dono da conta/unidade)
    AFILIADO,    // Parceiro de vendas
    PORTARIA     // Funcionário
}