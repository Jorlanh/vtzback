package com.votzz.backend.dto;

public record ResidentRegisterRequest(
    String nome,
    String email,
    String password,
    String cpf,
    String whatsapp,      // [NOVO]
    String unidade,
    String bloco,         // [NOVO]
    String condoIdentifier, // [NOVO] Nome ou CNPJ digitado pelo morador
    String secretKeyword
) {}