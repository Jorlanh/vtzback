package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "votes")
@EqualsAndHashCode(callSuper = true)
public class Vote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembly_id", nullable = false)
    private Assembly assembly;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- CORREÇÃO: Padronizado com o SQL (votes.option_id) ---
    @Column(name = "option_id", nullable = false) 
    private String optionId; 

    // --- CORREÇÃO: Padronizado com o SQL (votes.hash) ---
    @Column(name = "hash") 
    private String hash; 

    @Column(precision = 10, scale = 6)
    private BigDecimal fraction;
}