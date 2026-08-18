package com.votzz.backend.domain;

import com.votzz.backend.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Vinculo com Condomínio (Multi-tenant)
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // Dados do Pacote
    @Column(nullable = false)
    private String trackingCode; // Código de Rastreio

    private String origin; // Amazon, Mercado Livre, etc.
    
    @Column(nullable = false)
    private String recipientName; // Nome escrito na caixa

    private LocalDateTime arrivalDate; // Data que chegou na portaria

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // --- SNAPSHOT DO MORADOR (Dados imutáveis do momento do registro) ---
    @Column(nullable = false)
    private UUID residentId; // ID do usuário no sistema
    private String residentName;
    private String unit;     // Unidade
    private String block;    // Bloco
    private String residentEmail;
    private String residentCpf;
    private String residentWhatsapp;

    // --- ASSINATURAS ---
    
    // Assinatura Morador
    private LocalDateTime residentSignatureDate;
    private String residentSignatureName; // Nome de quem assinou (pode ser familiar)

    // Assinatura Portaria/Staff
    private LocalDateTime staffSignatureDate;
    private String staffSignatureName;

    // Auditoria
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = OrderStatus.PENDING;
    }
}