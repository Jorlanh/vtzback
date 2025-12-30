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
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
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
    private String unidade; // Este é o campo que o 'setUnit' do controller procura?
    private String bloco;
    
    // Se o seu código anterior usava "unit" em inglês, mantenha este campo também para compatibilidade:
    private String unit; 

    @Column(name = "booking_date")
    private LocalDate bookingDate;
    
    @Column(name = "start_time")
    private String startTime;
    
    @Column(name = "end_time")
    private String endTime;
    
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;
    
    @Column(name = "billing_type")
    private String billingType; // PIX, BOLETO

    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = "PENDING";
    }
}