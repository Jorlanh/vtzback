package com.votzz.backend.dto;

import java.util.List;
import java.util.UUID;

public class AuthDTOs {

    // Request agora aceita ID opcional do perfil
    public record LoginRequest(String login, String password, String selectedProfileId) {}

    // DTO para as opções de perfil
    public record ProfileOption(String id, String nome, String role, String tenantName) {}

    // Response atualizado
    public record LoginResponse(
        String token, 
        String type, 
        String id, 
        String nome, 
        String email, 
        String role, 
        String tenantId, 
        String bloco, 
        String unidade, 
        String cpf,
        boolean multipleProfiles, // Flag para o front
        List<ProfileOption> profiles // Lista de contas
    ) {}

    public record RegisterCondoRequest(
        String planId,
        String condoName,
        String cnpj,
        Integer qtyUnits,
        Integer qtyBlocks,
        String secretKeyword,
        
        String nameSyndic,
        String emailSyndic,
        String cpfSyndic,
        String whatsappSyndic,
        String passwordSyndic,
        
        String cep,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String pontoReferencia,

        String couponCode
    ) {}

    public record RegisterResponse(
        String message, 
        String redirectUrl,
        String pixPayload, 
        String pixImage    
    ) {}
    
    public record ResidentRegisterRequest(
        String nome,
        String email,
        String password,
        String cpf,
        String whatsapp,
        String unidade,
        String bloco,
        String condoIdentifier, 
        String secretKeyword    
    ) {}

    public record AffiliateRegisterRequest(String nome, String email, String cpf, String whatsapp, String password, String chavePix, String codigoAfiliado) {}
    public record ChangePasswordRequest(String oldPassword, String newPassword) {}
    public record ResetPasswordRequest(String code, String newPassword) {}
}