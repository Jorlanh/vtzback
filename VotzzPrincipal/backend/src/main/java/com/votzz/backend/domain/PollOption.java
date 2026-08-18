package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime; // Importante para o banco
import java.util.UUID;

@Data
@Entity
@Table(name = "poll_options") // Nome exato da tabela no SQL
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // O SQL tem 'label', então aqui TEM que ser 'label'
    @Column(nullable = false)
    private String label;

    // Campos de data que o SQL criou (para evitar erro de validação futura)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}