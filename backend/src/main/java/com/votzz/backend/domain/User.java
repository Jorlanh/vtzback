package com.votzz.backend.domain;

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

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String cpf;

    private String whatsapp;
    private String unidade;
    private String bloco;

    // Mantido para compatibilidade, mas o foco agora é a lista abaixo
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    // --- CORREÇÃO: Adicionando a lista de condomínios para suporte Multi-Tenant ---
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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (is2faEnabled == null) is2faEnabled = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == null) return List.of();
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
        return this.is2faEnabled;
    }
    
    public void setIs2faEnabled(Boolean is2faEnabled) {
        this.is2faEnabled = is2faEnabled;
    }
}