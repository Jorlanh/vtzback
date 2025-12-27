package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "afiliados")
public class Afiliado { // NÃO ESTENDE BaseEntity

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "codigo_ref")
    private String codigoRef;

    @Column(name = "chave_pix")
    private String chavePix;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Se precisar de tenant_id opcional:
    // @Column(name = "tenant_id")
    // private UUID tenantId;
}