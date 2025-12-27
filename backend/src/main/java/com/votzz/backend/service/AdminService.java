package com.votzz.backend.service;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.Plano;
import com.votzz.backend.dto.AdminOverviewDTO;
import com.votzz.backend.dto.TopAfiliadoDTO;
import com.votzz.backend.repository.ComissaoRepository;
import com.votzz.backend.repository.TenantRepository; // Agora vai funcionar
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final TenantRepository tenantRepository;
    private final ComissaoRepository comissaoRepository;

    public AdminOverviewDTO getOverview() {
        // 1. Total de condomínios ativos
        long totalCondominios = tenantRepository.countByAtivoTrue();
        
        // 2. MRR Real (Receita Mensal Recorrente)
        BigDecimal mrr = calcularMRR(); 
        
        // 3. Condomínios (usando paginação simples para pegar os primeiros)
        List<Tenant> condominiosVencendo = tenantRepository.findAll(PageRequest.of(0, 5)).getContent(); 
        
        // 4. Top Afiliados (Real)
        List<TopAfiliadoDTO> topAfiliados = comissaoRepository.findTopPerformers(PageRequest.of(0, 10));

        return new AdminOverviewDTO(totalCondominios, mrr, condominiosVencendo, topAfiliados);
    }
    
    private BigDecimal calcularMRR() {
        List<Tenant> ativos = tenantRepository.findByAtivoTrue();
        
        BigDecimal totalMrr = BigDecimal.ZERO;
        
        for (Tenant t : ativos) {
            if (t.getPlano() != null && t.getPlano().getPrecoBase() != null) {
                BigDecimal preco = t.getPlano().getPrecoBase();
                
                // Normaliza para valor mensal
                if (t.getPlano().getCiclo() == Plano.Ciclo.TRIMESTRAL) {
                    // Preço / 3
                    totalMrr = totalMrr.add(preco.divide(new BigDecimal("3"), 2, RoundingMode.HALF_EVEN));
                } else {
                    // Anual: Preço / 12
                    totalMrr = totalMrr.add(preco.divide(new BigDecimal("12"), 2, RoundingMode.HALF_EVEN));
                }
            }
        }
        return totalMrr;
    }
}