package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "votes")
@EqualsAndHashCode(callSuper = true)
public class Vote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembly_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private Assembly assembly;

    // CORREÇÃO AQUI: Ignora as propriedades do Proxy do Hibernate no User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) 
    private User user;

    @Column(name = "option_id", nullable = false) 
    private String optionId; 

    @Column(name = "hash") 
    private String hash; 

    @Column(precision = 10, scale = 6)
    private BigDecimal fraction;
}