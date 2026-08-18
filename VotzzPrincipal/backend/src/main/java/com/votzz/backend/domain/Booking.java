package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "reservations")
@EqualsAndHashCode(callSuper = true)
public class Booking extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "area_id") 
    private CommonArea commonArea;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // --- Dados do Morador (Snapshot para histórico) ---
    private String nome;
    private String cpf;
    private String whatsapp;
    
    // No banco você tem 'unidade', 'bloco' e 'unit'. 
    // Vamos mapear todos para garantir.
    private String unidade; 
    private String bloco;
    private String unit; 

    @Column(name = "booking_date")
    private LocalDate bookingDate;
    
    @Column(name = "start_time")
    private String startTime;
    
    @Column(name = "end_time")
    private String endTime;
    
    private String status; // PENDING, APPROVED, ETC.

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;
    
    @Column(name = "billing_type")
    private String billingType; 

    @Column(name = "receipt_url")
    private String receiptUrl; 

    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = "PENDING";
    }
}