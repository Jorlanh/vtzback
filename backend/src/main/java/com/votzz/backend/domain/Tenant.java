package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tenants")
public class Tenant { 

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String cnpj;

    private boolean ativo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // --- Campos SaaS ---

    @ManyToOne
    @JoinColumn(name = "plano_id")
    private Plano plano;

    @ManyToOne
    @JoinColumn(name = "afiliado_id")
    private Afiliado afiliado;

    @Column(name = "asaas_customer_id")
    private String asaasCustomerId;

    @Column(name = "asaas_wallet_id")
    private String asaasWalletId;

    @Column(name = "unidades_total")
    private Integer unidadesTotal;

    @Column(name = "data_expiracao_plano")
    private LocalDate dataExpiracaoPlano;

    @Column(name = "secret_keyword")
    private String secretKeyword;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // [NOVO] Método auxiliar para saber se está vencido
    public boolean isSubscriptionActive() {
        if (this.dataExpiracaoPlano == null) return false;
        // Retorna TRUE se a data de expiração for HOJE ou DEPOIS de hoje
        return !this.dataExpiracaoPlano.isBefore(LocalDate.now());
    }

    // [NOVO] Lógica Inteligente de Renovação (Soma dias ou Reinicia)
    public void renovarAssinatura(int mesesParaAdicionar) {
        LocalDate hoje = LocalDate.now();

        if (this.dataExpiracaoPlano == null || this.dataExpiracaoPlano.isBefore(hoje)) {
            // Se já venceu (ex: venceu dia 07/01/26 e hoje é 08/01/26), 
            // a nova data começa a contar de HOJE.
            this.dataExpiracaoPlano = hoje.plusMonths(mesesParaAdicionar);
        } else {
            // Se ainda não venceu (ex: vence dia 07/01/26 e hoje é 30/12/25),
            // soma 12 meses na data FUTURA (vira 07/01/27).
            this.dataExpiracaoPlano = this.dataExpiracaoPlano.plusMonths(mesesParaAdicionar);
        }
        
        this.ativo = true; // Garante que reativa se estava suspenso
    }
}