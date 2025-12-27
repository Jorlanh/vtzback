package com.votzz.backend.service;

import com.votzz.backend.domain.Booking;
import com.votzz.backend.domain.Plano;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.dto.BookingRequest.CreditCardDTO; // Import do DTO
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

    // Assinatura atualizada para receber dados de pagamento
    public Booking criarReserva(
            UUID tenantId, 
            Booking reserva, 
            String payerAsaasId,
            String billingType, 
            CreditCardDTO cardData
    ) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));
            
        BigDecimal taxaVotzz = BigDecimal.ZERO;
        
        // Verifica se cobra taxa (Plano Trimestral)
        if (tenant.getPlano() != null && tenant.getPlano().getCiclo() == Plano.Ciclo.TRIMESTRAL) {
             taxaVotzz = tenant.getPlano().getTaxaServicoReserva();
        }
        
        // CORREÇÃO: Passando novos parâmetros (billingType, cardData e o User dono da reserva)
        String paymentId = asaasClient.criarCobrancaSplit(
            payerAsaasId,                 
            reserva.getTotalPrice(),      
            tenant.getAsaasWalletId(),    
            taxaVotzz,
            billingType,                  // PIX, CREDIT_CARD...
            cardData,                     // Dados do cartão
            reserva.getUser()             // Dados do morador para antifraude
        );
        
        reserva.setAsaasPaymentId(paymentId);
        reserva.setStatus("PENDENTE");
        reserva.setTenant(tenant); 
        
        return bookingRepository.save(reserva);
    }
}