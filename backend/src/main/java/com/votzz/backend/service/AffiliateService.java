package com.votzz.backend.service;

import com.votzz.backend.domain.Afiliado;
import com.votzz.backend.domain.Comissao;
import com.votzz.backend.domain.StatusComissao; // Importar o Enum que criamos
import com.votzz.backend.integration.AsaasClient; // Mudado de AsaasService para AsaasClient (padrão do projeto)
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
    private final AsaasClient asaasClient; // Certifique-se que o nome da classe de integração é AsaasClient

    @Transactional(readOnly = true)
    public DashboardDTO getDashboard(UUID afiliadoId) {
        Afiliado afiliado = afiliadoRepository.findById(afiliadoId)
            .orElseThrow(() -> new RuntimeException("Afiliado não encontrado"));

        BigDecimal disponivel = comissaoRepository.sumSaldoDisponivel(afiliadoId);
        BigDecimal futuro = comissaoRepository.sumSaldoFuturo(afiliadoId);
        
        String link = "https://votzz.com/register?ref=" + afiliado.getCodigoRef();

        return new DashboardDTO(disponivel, futuro, link);
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processarPagamentosAutomaticos() {
        log.info("Iniciando rotina de pagamentos automáticos...");
        List<Afiliado> afiliados = afiliadoRepository.findAll();

        for (Afiliado af : afiliados) {
            BigDecimal saldoDisponivel = comissaoRepository.sumSaldoDisponivel(af.getId());

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
            c.setStatus(StatusComissao.PAGO); // Uso correto do Enum
            c.setAsaasTransferId(transferId);
        }
        comissaoRepository.saveAll(pagas);
    }
    
    public record DashboardDTO(BigDecimal saldoDisponivel, BigDecimal saldoFuturo, String linkIndicacao) {}
}