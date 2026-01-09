package com.votzz.backend.domain;

import com.votzz.backend.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
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

    @Column(unique = true)
    private String cpf;

    private String whatsapp;
    private String unidade;
    private String bloco;

    // Vinculação com Condomínio
    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "tenant_id")     
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // --- NOVO: Campo para suspensão de conta ---
    @Column(nullable = false)
    private boolean enabled = true; 

    @Column(name = "is_2fa_enabled")
    private boolean is2faEnabled = false;

    @Column(name = "secret_2fa")
    private String secret2fa;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- LÓGICA DE PERMISSÃO ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == null) return List.of();
        
        return List.of(
            new SimpleGrantedAuthority(this.role.name()),          // Ex: "ADMIN"
            new SimpleGrantedAuthority("ROLE_" + this.role.name()) // Ex: "ROLE_ADMIN"
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

    // --- CORREÇÃO: Usa o campo enabled do banco ---
    @Override
    public boolean isEnabled() { return enabled; }
}