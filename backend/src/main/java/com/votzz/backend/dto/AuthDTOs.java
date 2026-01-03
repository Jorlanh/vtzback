package com.votzz.backend.dto;

import com.votzz.backend.domain.enums.Role;
import java.util.UUID;
import java.util.Map;

public class AuthDTOs {

    public record LoginRequest(String login, String password) {}

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
        String cpf
    ) {}

    public record RegisterCondoRequest(
        String planId,
        String condoName,
        String cnpj,
        Integer qtyUnits,
        Integer qtyBlocks,
        String secretKeyword,
        
        // Síndico
        String nameSyndic,
        String emailSyndic,
        String cpfSyndic,
        String whatsappSyndic,
        String passwordSyndic,
        
        // Endereço
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
    
    // MANTIDO: Registro de Morador
    public record ResidentRegisterRequest(
        String nome,
        String email,
        String password,
        String cpf,
        String whatsapp,
        String unidade,
        String bloco,
        String condoIdentifier, // CNPJ ou Nome do Condomínio
        String secretKeyword    // Palavra-chave do Condomínio
    ) {}

    // MANTIDOS: Outros DTOs
    public record AffiliateRegisterRequest(String nome, String email, String cpf, String whatsapp, String password, String chavePix, String codigoAfiliado) {}
    public record ChangePasswordRequest(String oldPassword, String newPassword) {}
    public record ResetPasswordRequest(String code, String newPassword) {}
}