package com.votzz.backend.service;

import com.votzz.backend.domain.Booking;
import com.votzz.backend.domain.Plano;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.integration.AsaasClient;
import com.votzz.backend.repository.BookingRepository;
import com.votzz.backend.repository.TenantRepository;
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

    // Assinatura atualizada: Removido cardData pois não usamos mais cartão
    public Booking criarReserva(
            UUID tenantId, 
            Booking reserva, 
            String payerAsaasId,
            String billingType
    ) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));
            
        BigDecimal taxaVotzz = BigDecimal.ZERO;
        
        // Verifica se cobra taxa (Plano Trimestral)
        if (tenant.getPlano() != null && tenant.getPlano().getCiclo() == Plano.Ciclo.TRIMESTRAL) {
             taxaVotzz = tenant.getPlano().getTaxaServicoReserva();
        }
        
        // CORREÇÃO: Chamada simplificada sem cartão
        String paymentId = asaasClient.criarCobrancaSplit(
            payerAsaasId,                 
            reserva.getTotalPrice(),      
            tenant.getAsaasWalletId(),    
            taxaVotzz,
            billingType                   // PIX ou BOLETO
        );
        
        reserva.setAsaasPaymentId(paymentId);
        reserva.setStatus("PENDENTE");
        reserva.setTenant(tenant); 
        
        return bookingRepository.save(reserva);
    }
}