package com.votzz.backend.dto;

import java.util.List;

public class AuthDTOs {

    // Request atualizado com deviceId, trustDevice e keepLogged
    public record LoginRequest(
        String login, 
        String password, 
        String selectedProfileId,
        Integer code2fa, 
        String deviceId,    // ID do navegador/dispositivo
        boolean trustDevice, // Checkbox "Não pedir novamente por 30 dias" (2FA)
        boolean keepLogged   // Checkbox "Mantenha-me conectado" (Sessão longa)
    ) {}

    // DTO para as opções de perfil (Leque)
    public record ProfileOption(String userId, String role, String contextName, String userName) {}

    // DTO auxiliar para unidades
    public record UnitDTO(String bloco, String unidade) {}

    // --- CORREÇÃO: Transformado em CLASS para suportar múltiplos construtores (Sobrecarga) ---
    public static class LoginResponse {
        public String token;
        public String type = "Bearer";
        public String id;
        public String nome;
        public String email;
        public String role;
        public String tenantId;
        public String tenantName; // Nome do condomínio para exibir no Dashboard
        public String bloco;
        public String unidade;
        public String cpf;
        
        // --- NOVO CAMPO: Lista de unidades para o Frontend exibir o modal ---
        public List<String> unidadesList; 

        public boolean multipleProfiles;
        public boolean requiresTwoFactor;
        public boolean is2faSetup; // Para o primeiro acesso
        public List<String> backupCodes;
        public List<ProfileOption> profiles;

        // Construtor 1: Login com Sucesso (Token gerado)
        public LoginResponse(String token, String type, String id, String nome, String email, 
                             String role, String tenantId, String tenantName, 
                             String bloco, String unidade, String cpf,
                             List<String> unidadesList, // <--- Recebe a lista aqui
                             boolean requiresTwoFactor, boolean is2faSetup,
                             List<ProfileOption> profiles) {
            this.token = token;
            this.type = type;
            this.id = id;
            this.nome = nome;
            this.email = email;
            this.role = role;
            this.tenantId = tenantId;
            this.tenantName = tenantName;
            this.bloco = bloco;
            this.unidade = unidade;
            this.cpf = cpf;
            this.unidadesList = unidadesList; // <--- Atribui a lista
            this.requiresTwoFactor = requiresTwoFactor;
            this.is2faSetup = is2faSetup;
            this.profiles = profiles;
            this.multipleProfiles = false;
        }

        // Construtor 2: Retornar lista de perfis ("Leque") - SEM TOKEN
        public LoginResponse(boolean multipleProfiles, List<ProfileOption> profiles) {
            this.multipleProfiles = multipleProfiles;
            this.profiles = profiles;
        }
    }

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
        String cycle, // Campo necessário para o ciclo de pagamento
        
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

        String couponCode,   // Cupom de Desconto
        String affiliateCode // Novo campo: Código do Afiliado
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