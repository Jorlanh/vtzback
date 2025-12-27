package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "votes")
@EqualsAndHashCode(callSuper = true)
public class Vote extends BaseEntity { // Herda createdAt da BaseEntity

    @ManyToOne
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "assembly_id")
    private Assembly assembly;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "opcao_escolhida", nullable = false)
    private String opcaoEscolhida;

    @Column(name = "audit_hash")
    private String auditHash;
}