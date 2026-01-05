package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "poll_options_assembly") // <--- AQUI ESTAVA O ERRO (MUDE PARA poll_options_assembly)
@EqualsAndHashCode(callSuper = true)
public class VoteOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @JsonIgnore
    @ToString.Exclude
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembly_id")
    @JsonBackReference
    @ToString.Exclude
    private Assembly assembly;

    @Column(nullable = false)
    private String descricao;
}