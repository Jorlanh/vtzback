package com.votzz.backend.service;

import com.votzz.backend.domain.Plano.Ciclo;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class PlanoService {

    // Retorna um objeto simples com o preço calculado
    public record SugestaoPlano(String nome, BigDecimal precoFinal, Ciclo ciclo) {}

    public SugestaoPlano calcularPlano(int qtdUnidades, BigDecimal arrecadacaoMensal, Ciclo ciclo) {
        String nomePlano;
        BigDecimal precoMensal;

        // Regra A: Definição Base
        if (qtdUnidades <= 30) {
            nomePlano = "Essencial";
            precoMensal = new BigDecimal("190.00");
        } else if (qtdUnidades <= 80) {
            nomePlano = "Business";
            precoMensal = new BigDecimal("350.00");
        } else {
            nomePlano = "Custom";
            BigDecimal base = new BigDecimal("300.00");
            
            // R$ 2 por unidade acima de 80
            BigDecimal extraUnidades = BigDecimal.valueOf(qtdUnidades - 80).multiply(new BigDecimal("2.00"));
            
            // Revenue Share: 0.5% da arrecadação (Cap R$ 1000)
            BigDecimal revenueShare = arrecadacaoMensal.multiply(new BigDecimal("0.005"));
            if (revenueShare.compareTo(new BigDecimal("1000.00")) > 0) {
                revenueShare = new BigDecimal("1000.00");
            }
            
            precoMensal = base.add(extraUnidades).add(revenueShare);
        }

        // Regra B: Ciclos e Descontos
        BigDecimal precoFinal;
        if (ciclo == Ciclo.TRIMESTRAL) {
            precoFinal = precoMensal.multiply(new BigDecimal("3"));
        } else {
            // Anual: (Mensal * 12) * 0.8 -> 20% OFF
            precoFinal = precoMensal.multiply(new BigDecimal("12")).multiply(new BigDecimal("0.8"));
        }

        return new SugestaoPlano(nomePlano, precoFinal, ciclo);
    }
}