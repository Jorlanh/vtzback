package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tenants")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String cnpj;

    private boolean ativo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- ENDEREÇO ---
    private String cep;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String estado;
    
    @Column(name = "ponto_referencia")
    private String pontoReferencia;

    // --- Campos SaaS ---
    @Column(name = "unidades_total")
    private Integer unidadesTotal;

    @Column(name = "blocos_total")
    private Integer blocos;

    // CORREÇÃO AQUI: Ignora as propriedades do Proxy do Hibernate
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) 
    private Plano plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "afiliado_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Afiliado afiliado;

    // --- INTEGRAÇÕES & DADOS BANCÁRIOS ---
    @Column(name = "asaas_customer_id")
    private String asaasCustomerId;

    @Column(name = "asaas_wallet_id")
    private String asaasWalletId;
    
    @Column(name = "banco_nome")
    private String banco;

    @Column(name = "banco_agencia")
    private String agencia;

    @Column(name = "banco_conta")
    private String conta;

    @Column(name = "chave_pix")
    private String chavePix;
    
    @Column(name = "kiwify_transaction_id")
    private String kiwifyTransactionId;

    @Column(name = "status_assinatura")
    private String statusAssinatura;

    @Column(name = "data_expiracao_plano")
    private LocalDate dataExpiracaoPlano;

    @Column(name = "secret_keyword")
    private String secretKeyword;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isSubscriptionActive() {
        if (this.dataExpiracaoPlano == null) return false;
        return !this.dataExpiracaoPlano.isBefore(LocalDate.now());
    }

    public void renovarAssinatura(int mesesParaAdicionar) {
        LocalDate hoje = LocalDate.now();
        if (this.dataExpiracaoPlano == null || this.dataExpiracaoPlano.isBefore(hoje)) {
            this.dataExpiracaoPlano = hoje.plusMonths(mesesParaAdicionar);
        } else {
            this.dataExpiracaoPlano = this.dataExpiracaoPlano.plusMonths(mesesParaAdicionar);
        }
        this.ativo = true;
        this.statusAssinatura = "ACTIVE";
    }
}