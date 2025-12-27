package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tenants")
public class Tenant { // REMOVIDO: extends BaseEntity

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String cnpj;

    private boolean ativo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // --- Campos SaaS ---

    @ManyToOne
    @JoinColumn(name = "plano_id")
    private Plano plano;

    @Column(name = "asaas_customer_id")
    private String asaasCustomerId;

    @Column(name = "asaas_wallet_id")
    private String asaasWalletId;

    @Column(name = "unidades_total")
    private Integer unidadesTotal;

    @Column(name = "data_expiracao_plano")
    private LocalDate dataExpiracaoPlano;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}