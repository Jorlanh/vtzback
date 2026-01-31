package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "tenant_payment_config")
@EqualsAndHashCode(callSuper = true)
public class TenantPaymentConfig extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    // Configurações de Ativação
    private boolean enableAsaas = false;
    private boolean enableManualPix = true;

    // Dados Bancários Manuais
    private String bankName;
    private String agency;
    private String account;
    private String pixKey;
    
    @Column(columnDefinition = "TEXT")
    private String instructions;

    // --- CORREÇÃO: Renomeado para bater com o Service (getAsaasAccessToken) ---
    @Column(columnDefinition = "TEXT")
    private String asaasAccessToken; 

    // Mantido para uso futuro ou controle interno
    private String asaasWalletId;
}