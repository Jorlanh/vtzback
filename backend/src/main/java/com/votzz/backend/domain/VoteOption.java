package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore; // <--- Importante
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString; // <--- Importante para evitar loop no log

@Data
@Entity
@Table(name = "poll_options")
@EqualsAndHashCode(callSuper = true)
public class VoteOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @JsonIgnore // <--- OBRIGATÓRIO: Corta o peso do JSON. A assembleia já tem o tenant.
    @ToString.Exclude // Evita loop se você usar System.out.println
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembly_id")
    @JsonBackReference // Mantém isso para evitar o loop Assembly <-> Option
    @ToString.Exclude
    private Assembly assembly;

    @Column(nullable = false)
    private String descricao;
}