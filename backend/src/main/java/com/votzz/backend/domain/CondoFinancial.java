package com.votzz.backend.domain;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal; // Importe o BigDecimal
import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class CondoFinancial extends BaseEntity {
    private BigDecimal balance; // Mude de Double para BigDecimal
    private LocalDateTime lastUpdate;
    private String updatedBy;
}