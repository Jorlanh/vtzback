package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "assemblies")
@EqualsAndHashCode(callSuper = true)
public class Assembly extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "description") 
    private String descricao;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDateTime dataFim;

    @Column(name = "link_video_conferencia")
    private String linkVideoConferencia;

    // --- CORREÇÃO: Uso de Enum em vez de String ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusAssembly status; 

    @Column(name = "anexo_url")
    private String anexoUrl;

    @Column(name = "tipo_assembleia")
    private String tipoAssembleia; 

    @Column(name = "quorum_type")
    private String quorumType;

    @Column(name = "vote_type")
    private String voteType;

    @Column(name = "vote_privacy")
    private String votePrivacy;

    // --- DEFINIÇÃO DO ENUM ---
    public enum StatusAssembly {
        AGENDADA, ABERTA, ENCERRADA, CANCELADA
    }
}