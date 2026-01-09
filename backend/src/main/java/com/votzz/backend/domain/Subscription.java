package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    private PaymentProvider provider; // KIWIFY, ASAAS

    @Column(unique = true)
    private String externalReference; // ID da Transação/Ordem no Gateway

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status; // PENDING, ACTIVE, OVERDUE, CANCELED

    private String planType; // ESSENCIAL, BUSINESS, CUSTOM
    
    private BigDecimal amount;
    
    private LocalDateTime nextBillingDate;
    private LocalDateTime createdAt;

    public enum PaymentProvider { KIWIFY, ASAAS }
    public enum SubscriptionStatus { PENDING, ACTIVE, OVERDUE, CANCELED }
    
    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}