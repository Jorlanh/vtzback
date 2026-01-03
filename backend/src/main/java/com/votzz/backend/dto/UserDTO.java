package com.votzz.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String nome;
    private String email;
    private String role;
    private String condominio;
    private LocalDateTime lastSeen;
    private String token; // Mantido para compatibilidade com Login, pode ser null aqui
    private String bloco;
    private String unidade;
    private UUID tenantId;
    private String cpf;      
    private String whatsapp; 

    // Construtor compacto que você pediu para o AdminService (caso use queries específicas)
    public UserDTO(UUID id, String nome, String email, String role, String condominio, LocalDateTime lastSeen) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.role = role;
        this.condominio = condominio;
        this.lastSeen = lastSeen;
    }
}