package com.votzz.backend.dto;

import java.util.List;
import java.util.UUID;

public class AuthDTOs {

    // Request atualizado com deviceId e trustDevice para a lógica de "Lembrar navegador"
    public record LoginRequest(
        String login, 
        String password, 
        String selectedProfileId,
        Integer code2fa, 
        String deviceId,    // ID do navegador/dispositivo
        boolean trustDevice // Checkbox "Não pedir novamente por 30 dias"
    ) {}

    // DTO para as opções de perfil
    public record ProfileOption(String id, String nome, String role, String tenantName) {}

    // DTO auxiliar para unidades
    public record UnitDTO(String bloco, String unidade) {}

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
        boolean multipleProfiles, 
        boolean requiresTwoFactor, // Flag para avisar o front que precisa do código
        List<ProfileOption> profiles 
    ) {}

    // NOVOS DTOs PARA O SETUP DO 2FA
    public record TwoFactorSetupResponse(String secret, String qrCodeUrl) {}
    public record TwoFactorConfirmRequest(String code) {}

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
    
    // ATUALIZADO: Aceita lista de units
    public record ResidentRegisterRequest(
        String nome,
        String email,
        String password,
        String cpf,
        String whatsapp,
        // Mantemos estes para compatibilidade se o front mandar só um
        String unidade, 
        String bloco,
        // Novo campo
        List<UnitDTO> units, 
        String condoIdentifier, 
        String secretKeyword    
    ) {}

    public record AffiliateRegisterRequest(String nome, String email, String cpf, String whatsapp, String password, String chavePix, String codigoAfiliado) {}
    public record ChangePasswordRequest(String oldPassword, String newPassword) {}
    public record ResetPasswordRequest(String code, String newPassword) {}
}