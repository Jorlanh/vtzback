package com.votzz.backend.service;

import com.votzz.backend.domain.Afiliado;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.Comissao;
import com.votzz.backend.domain.StatusComissao; 
import com.votzz.backend.integration.AsaasClient;
import com.votzz.backend.repository.AfiliadoRepository;
import com.votzz.backend.repository.ComissaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AffiliateService {

    private final ComissaoRepository comissaoRepository;
    private final AfiliadoRepository afiliadoRepository;
    private final AsaasClient asaasClient; 

    @Transactional(readOnly = true)
    public DashboardDTO getDashboard(User user) {
        // Busca o afiliado vinculado ao usuário logado
        Afiliado afiliado = afiliadoRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Conta de afiliado não encontrada."));

        UUID afiliadoId = afiliado.getId();

        // [CORREÇÃO] Tratamento de nulos caso não haja comissões ainda
        BigDecimal disponivel = comissaoRepository.sumSaldoDisponivel(afiliadoId);
        if (disponivel == null) disponivel = BigDecimal.ZERO;

        BigDecimal futuro = comissaoRepository.sumSaldoFuturo(afiliadoId);
        if (futuro == null) futuro = BigDecimal.ZERO;
        
        // Se usar link em produção, alterar para o domínio real
        String link = "https://votzz.com/register-condo?ref=" + afiliado.getCodigoRef();

        return new DashboardDTO(disponivel, futuro, link);
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processarPagamentosAutomaticos() {
        log.info("Iniciando rotina de pagamentos automáticos...");
        List<Afiliado> afiliados = afiliadoRepository.findAll();

        for (Afiliado af : afiliados) {
            BigDecimal saldoDisponivel = comissaoRepository.sumSaldoDisponivel(af.getId());
            if (saldoDisponivel == null) saldoDisponivel = BigDecimal.ZERO;

            if (saldoDisponivel.compareTo(new BigDecimal("30.00")) >= 0) {
                try {
                    log.info("Processando pagamento de R$ {} para {}", saldoDisponivel, af.getCodigoRef());
                    String transferId = asaasClient.transferirPix(af.getChavePix(), saldoDisponivel);
                    baixarComissoesPagas(af.getId(), transferId);
                    log.info("Pagamento realizado com sucesso! ID Asaas: {}", transferId);
                } catch (Exception e) {
                    log.error("Falha ao pagar afiliado {}: {}", af.getCodigoRef(), e.getMessage());
                }
            }
        }
    }

    private void baixarComissoesPagas(UUID afiliadoId, String transferId) {
        List<Comissao> pagas = comissaoRepository.findLiberadasParaSaque(afiliadoId, LocalDate.now());
        for (Comissao c : pagas) {
            c.setStatus(StatusComissao.PAGO);
            c.setAsaasTransferId(transferId);
        }
        comissaoRepository.saveAll(pagas);
    }
    
    public record DashboardDTO(BigDecimal saldoDisponivel, BigDecimal saldoFuturo, String linkIndicacao) {}
}