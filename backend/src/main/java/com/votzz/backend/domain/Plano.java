package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "planos")
public class Plano { // REMOVIDO: extends BaseEntity

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome; // Essencial, Business, Custom

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Ciclo ciclo; // TRIMESTRAL, ANUAL

    @Column(name = "max_unidades")
    private Integer maxUnidades;
    
    @Column(name = "preco_base")
    private BigDecimal precoBase;
    
    @Column(name = "taxa_servico_reserva")
    private BigDecimal taxaServicoReserva; // Quanto o Votzz retém por reserva

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum Ciclo {
        TRIMESTRAL, ANUAL
    }
}