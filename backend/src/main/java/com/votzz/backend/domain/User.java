package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "users")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String cpf;
    
    private String unidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; 

    private String whatsapp;

    // --- CORREÇÃO DO ERRO ---
    // Adicionado insertable = false, updatable = false
    // Isso diz ao Hibernate: "Use o tenantId da BaseEntity para salvar, e use este campo apenas para ler o objeto Tenant"
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;
}