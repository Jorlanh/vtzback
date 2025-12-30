package com.votzz.backend.dto;

import java.util.UUID;

import com.votzz.backend.domain.enums.Role;

public class AuthDTOs {

    // [ATUALIZADO] Mudamos de 'email' para 'login' para aceitar CPF também
    public record LoginRequest(String login, String password) {}

    public record LoginResponse(
        String token, 
        String nome, 
        String email, 
        Role role, 
        UUID tenantId,
        UUID id
    ) {}

    public record ForgotPasswordRequest(String email) {}

    public record ResetPasswordRequest(String token, String newPassword) {}
}