package com.votzz.backend.dto; // Confira seu pacote

import lombok.Data;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String nome;
    private String email;
    private String role; // MORADOR, SINDICO, etc.
    
    // --- ADICIONE ISTO ---
    private String bloco;
    private String unidade;
    // ---------------------
    
    private UUID tenantId;
}