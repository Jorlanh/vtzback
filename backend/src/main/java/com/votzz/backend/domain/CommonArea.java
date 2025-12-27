package com.votzz.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Entity
@Table(name = "common_area")
@Data
@EqualsAndHashCode(callSuper = true)
public class CommonArea extends BaseEntity {
    private String name;
    private String type;
    private Integer capacity;
    private String description;
    private BigDecimal price;
    private Boolean requiresApproval;
    private String openTime;
    private String closeTime;
    private String imageUrl;
}