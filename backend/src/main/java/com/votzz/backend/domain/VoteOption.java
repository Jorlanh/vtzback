package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "poll_options") // <--- CORREÇÃO: O nome no banco é 'poll_options'
@EqualsAndHashCode(callSuper = true)
public class VoteOption extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "assembly_id")
    private Assembly assembly;

    @Column(nullable = false)
    private String descricao;
}