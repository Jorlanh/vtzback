package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "common_areas")
@Data
@EqualsAndHashCode(callSuper = true)
public class CommonArea extends BaseEntity {

    // AGORA SIM: Este é o único lugar mapeando a coluna "tenant_id"
    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    private String name;
    private String type;
    private Integer capacity;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String rules; 

    private BigDecimal price;
    private Boolean requiresApproval;
    private String openTime;
    private String closeTime;
    private String imageUrl;

    // Este método envia o ID no JSON para o frontend, 
    // mas não interfere no banco de dados.
    public UUID getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }
}