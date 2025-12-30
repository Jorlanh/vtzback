package com.votzz.backend.service;

import com.votzz.backend.domain.Plano.Ciclo;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PlanoService {

    public record SugestaoPlano(
        String nome, 
        BigDecimal precoMensalBase, 
        BigDecimal precoFinal, 
        Ciclo ciclo, 
        String descricaoPreco
    ) {}

    public SugestaoPlano calcularPlano(int qtdUnidades, Ciclo ciclo) {
        String nomePlano;
        BigDecimal precoMensal;

        // --- REGRA 1: Definição do Plano Base ---
        if (qtdUnidades <= 30) {
            // Plano Essencial (Até 30 un)
            nomePlano = "Essencial";
            precoMensal = new BigDecimal("190.00");
        } else if (qtdUnidades <= 80) {
            // Plano Business (31 a 80 un)
            nomePlano = "Business";
            precoMensal = new BigDecimal("490.00");
        } else {
            // Plano Custom (81+ un)
            nomePlano = "Custom";
            BigDecimal base = new BigDecimal("490.00");
            
            // Regra: R$ 490 base + R$ 2,50 por unidade ADICIONAL acima de 80
            // Isso evita que o condomínio pule de R$ 490 para R$ 692 só por ter 81 unidades.
            // Se tiver 81 unidades, paga R$ 490 + R$ 2,50 = R$ 492,50.
            int unidadesExtras = qtdUnidades - 80;
            BigDecimal custoExtra = BigDecimal.valueOf(unidadesExtras).multiply(new BigDecimal("2.50"));
            
            precoMensal = base.add(custoExtra);
        }

        // --- REGRA 2: Cálculo do Ciclo (Trimestral vs Anual) ---
        BigDecimal precoFinal;
        String descricao;

        if (ciclo == Ciclo.TRIMESTRAL) {
            // Trimestral: Valor Mensal * 3 (Sem desconto)
            precoFinal = precoMensal.multiply(new BigDecimal("3"));
            descricao = "Cobrado a cada 3 meses";
        } else {
            // Anual: (Valor Mensal * 12) com 20% de Desconto
            BigDecimal valorAnualBruto = precoMensal.multiply(new BigDecimal("12"));
            BigDecimal desconto = valorAnualBruto.multiply(new BigDecimal("0.20")); // 20% OFF
            precoFinal = valorAnualBruto.subtract(desconto);
            descricao = "Cobrado anualmente (20% de desconto aplicado)";
        }

        return new SugestaoPlano(
            nomePlano, 
            precoMensal, 
            precoFinal.setScale(2, RoundingMode.HALF_EVEN), 
            ciclo, 
            descricao
        );
    }
}