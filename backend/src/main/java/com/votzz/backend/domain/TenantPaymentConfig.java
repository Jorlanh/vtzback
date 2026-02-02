package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.UUID;

@Data
@Entity
@Table(name = "tenant_payment_config")
@EqualsAndHashCode(callSuper = true)
public class TenantPaymentConfig extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant; // Certifique-se que este campo existe e é do tipo Tenant

    private boolean enableAsaas;
    private boolean enableManualPix;
    private String bankName;
    private String agency;
    private String account;
    private String pixKey;
    
    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(columnDefinition = "TEXT")
    private String asaasAccessToken;
}