package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private String token; // O código de 6 dígitos

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    // Construtor auxiliar
    public PasswordResetToken() {}
    
    public PasswordResetToken(User user, String token) {
        this.user = user;
        this.token = token;
        // O código expira em 15 minutos
        this.expiryDate = LocalDateTime.now().plusMinutes(15);
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}