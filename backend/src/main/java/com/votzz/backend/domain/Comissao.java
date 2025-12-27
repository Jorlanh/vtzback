package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "comissoes")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comissao { // REMOVIDO: extends BaseEntity

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "afiliado_id")
    private Afiliado afiliado;

    @ManyToOne
    @JoinColumn(name = "condominio_pagante_id")
    private Tenant condominioPagante;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "data_venda", nullable = false)
    private LocalDate dataVenda;

    @Column(name = "data_liberacao", nullable = false)
    private LocalDate dataLiberacao;

    @Enumerated(EnumType.STRING)
    private StatusComissao status;

    @Column(name = "asaas_transfer_id")
    private String asaasTransferId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}