package com.votzz.backend.dto;

import java.util.UUID;

public record ResidentRegisterRequest(
    String nome,
    String email,
    String password,
    String cpf,
    String unidade,
    String whatsapp,
    
    // --- Campos de Segurança do Condomínio ---
    UUID tenantId,       // O ID do condomínio selecionado na lista
    String tenantCnpj,   // O CNPJ digitado para conferência
    String secretKeyword // A palavra-chave criada pelo Síndico
) {}