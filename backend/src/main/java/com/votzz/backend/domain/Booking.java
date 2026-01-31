package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "reservations")
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditingEntityListener.class) // Necessário para @CreatedDate funcionar
public class Booking extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "tenant_id") // Removi insertable=false para facilitar persistência se necessário
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "area_id") 
    private CommonArea commonArea;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // --- Dados do Morador (Snapshot) ---
    private String nome;
    private String cpf;
    private String unidade;
    private String bloco;
    
    // Mantido para compatibilidade
    private String unit; 

    @Column(name = "booking_date")
    private LocalDate bookingDate;
    
    @Column(name = "start_time")
    private String startTime;
    
    @Column(name = "end_time")
    private String endTime;
    
    // Status: PENDING, UNDER_ANALYSIS, APPROVED, CONFIRMED, REJECTED, CANCELLED, EXPIRED
    private String status; 

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    // --- FINANCEIRO ---
    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;
    
    @Column(name = "billing_type")
    private String billingType; // "ASAAS_PIX", "PIX_MANUAL", "FREE"

    @Column(name = "receipt_url")
    private String receiptUrl; 

    // --- CONTROLE DE TEMPO (30 MIN) ---
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = "PENDING";
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}