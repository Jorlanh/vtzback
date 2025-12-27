package com.votzz.backend.service;

import com.votzz.backend.domain.Booking;
import com.votzz.backend.domain.Plano;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.integration.AsaasClient; // Importar o client do pacote integration
import com.votzz.backend.repository.BookingRepository;
import com.votzz.backend.repository.TenantRepository; // Import corrigido
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final TenantRepository tenantRepository;
    private final BookingRepository bookingRepository;
    private final AsaasClient asaasClient;

    public Booking criarReserva(UUID tenantId, Booking reserva, String payerAsaasId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));
            
        BigDecimal taxaVotzz = BigDecimal.ZERO;
        
        // Verifica se o tenant tem plano e se é trimestral para cobrar taxa
        if (tenant.getPlano() != null && tenant.getPlano().getCiclo() == Plano.Ciclo.TRIMESTRAL) {
             taxaVotzz = tenant.getPlano().getTaxaServicoReserva();
        }
        
        // CORREÇÃO DOS ARGUMENTOS:
        // Ordem esperada no AsaasClient: (payerId, valorTotal, walletCondominio, taxaVotzz)
        String paymentId = asaasClient.criarCobrancaSplit(
            payerAsaasId,                 // 1. Quem paga (Morador)
            reserva.getTotalPrice(),      // 2. Valor Total
            tenant.getAsaasWalletId(),    // 3. Carteira do Condomínio
            taxaVotzz                     // 4. Taxa do Votzz
        );
        
        // Agora funciona porque atualizamos a entidade Booking no passo 2
        reserva.setAsaasPaymentId(paymentId);
        reserva.setStatus("PENDENTE");
        reserva.setTenant(tenant); // Importante vincular o tenant
        
        return bookingRepository.save(reserva);
    }
}