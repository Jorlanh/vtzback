package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.votzz.backend.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String cpf;

    // --- CONTATOS ---
    private String whatsapp;
    private String phone; // Campo adicionado para compatibilidade com o Controller

    // Campos legados (Mantidos para compatibilidade)
    private String unidade;
    private String bloco;

    // --- NOVO CAMPO: LISTA DE UNIDADES ---
    // Isso cria uma tabela 'user_unidades' para guardar múltiplas unidades por usuário
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_unidades", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "unidade_info")
    private List<String> unidadesList = new ArrayList<>();

    // Mantido para compatibilidade legado, mas o JSON vai ignorar para evitar loops/erros
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id")
    @JsonIgnore 
    private Tenant tenant;

    // Lista principal para Multi-Tenant (Síndicos Profissionais)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_tenants",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "tenant_id")
    )
    private List<Tenant> tenants = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    // --- CAMPOS 2FA ---
    @Column(name = "is_2fa_enabled")
    private Boolean is2faEnabled = false;

    @Column(name = "secret_2fa")
    private String secret2fa;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- TOKENS DE RECUPERAÇÃO ---
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (is2faEnabled == null) is2faEnabled = false;
        
        // Sincronia básica: se tiver tenant na lista mas não no singular, seta o singular
        if (tenant == null && !tenants.isEmpty()) {
            tenant = tenants.get(0);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == null) return List.of();
        // Retorna a role crua (ex: "SINDICO") E a role spring (ex: "ROLE_SINDICO") para garantir compatibilidade
        return List.of(
            new SimpleGrantedAuthority(this.role.name()),
            new SimpleGrantedAuthority("ROLE_" + this.role.name())
        );
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
    
    public Boolean getIs2faEnabled() {
        return this.is2faEnabled != null && this.is2faEnabled;
    }
    
    public void setIs2faEnabled(Boolean is2faEnabled) {
        this.is2faEnabled = is2faEnabled;
    }

    // --- GETTERS E SETTERS MANUAIS PARA GARANTIR ---
    public void setPhone(String phone) { this.phone = phone; }
    public String getPhone() { return phone; }
    
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getWhatsapp() { return whatsapp; }
}