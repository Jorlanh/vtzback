package com.votzz.backend.dto;

import com.votzz.backend.domain.Role;
import java.util.UUID;

public class AuthDTOs {

    // Para receber o login
    public record LoginRequest(String email, String password) {}

    // Para devolver após login bem-sucedido
    public record LoginResponse(
        String token, 
        String nome, 
        String email, 
        Role role, 
        UUID tenantId,
        UUID id
    ) {}

    // Para recuperação de senha
    public record ForgotPasswordRequest(String email) {}

    // Para resetar a senha com o código
    public record ResetPasswordRequest(String token, String newPassword) {}
}