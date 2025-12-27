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

    // Tenant (read-only)
    @ManyToOne
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    // CORRETO: Mapeando 'area_id' para o objeto CommonArea
    @ManyToOne
    @JoinColumn(name = "area_id") 
    private CommonArea commonArea;

    // Se houver algum campo antigo como este abaixo, APAGUE-O:
    // @Column(name = "area_comum")
    // private String areaComum;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String unit;
    
    @Column(name = "booking_date")
    private LocalDate bookingDate;
    
    @Column(name = "start_time")
    private String startTime;
    
    @Column(name = "end_time")
    private String endTime;
    
    private String status; 

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;
}