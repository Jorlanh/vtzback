package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "assemblies")
@EqualsAndHashCode(callSuper = true)
public class Assembly extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "description", columnDefinition = "TEXT") 
    private String description;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @Column(name = "link_video_conferencia")
    private String linkVideoConferencia;

    @Column(name = "youtube_live_url")
    private String youtubeLiveUrl;

    @Column(name = "relatorio_ia_url", columnDefinition = "TEXT")
    private String relatorioIaUrl;

    @Column(name = "status")
    private String status; 

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

    @OneToMany(mappedBy = "assembly", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference 
    private List<VoteOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "assembly", fetch = FetchType.LAZY)
    @JsonManagedReference 
    private List<Vote> votes = new ArrayList<>();
}