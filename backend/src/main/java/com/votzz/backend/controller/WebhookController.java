package com.votzz.backend.controller;

import com.votzz.backend.domain.Booking;
import com.votzz.backend.domain.Comissao;
import com.votzz.backend.domain.StatusComissao;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.repository.BookingRepository;
import com.votzz.backend.repository.ComissaoRepository;
import com.votzz.backend.repository.TenantRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/payment/webhook") // Padronizei a rota
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final TenantRepository tenantRepository;
    private final ComissaoRepository comissaoRepository;
    private final BookingRepository bookingRepository;

    @Value("${asaas.webhook.token}") 
    private String webhookSecretToken;

    @PostMapping
    @Transactional
    public ResponseEntity<String> handleAsaasWebhook(@RequestBody AsaasWebhookEvent event, 
                                                     @RequestHeader(value = "asaas-access-token", required = false) String token) {
        
        // 1. VALIDAÇÃO DE SEGURANÇA
        if (webhookSecretToken != null && !webhookSecretToken.equals(token)) {
            log.warn("Webhook negado: Token inválido.");
            return ResponseEntity.status(401).body("Acesso Negado");
        }

        log.info("Webhook recebido: Evento={} ID={}", event.event, event.payment.id);

        // 2. PROCESSAMENTO DE PAGAMENTO CONFIRMADO
        if ("PAYMENT_CONFIRMED".equals(event.event) || "PAYMENT_RECEIVED".equals(event.event)) {
            
            // Tenta processar como RESERVA DE ÁREA (Prioridade)
            boolean isBooking = processarReserva(event.payment);

            // Se NÃO for reserva, tenta processar como ASSINATURA SAAS (Gera Comissão)
            if (!isBooking) {
                processarComissaoAfiliado(event.payment);
            }
        }
        
        // 3. PROCESSAMENTO DE ESTORNO OU REPROVAÇÃO
        else if ("PAYMENT_REFUNDED".equals(event.event) || "PAYMENT_REPROVED".equals(event.event)) {
            cancelarReserva(event.payment);
        }

        return ResponseEntity.ok("Webhook Processed");
    }

    /**
     * Tenta localizar uma reserva pelo ID de pagamento e aprová-la.
     * @return true se encontrou e processou uma reserva.
     */
    private boolean processarReserva(AsaasPaymentData payment) {
        Booking booking = bookingRepository.findByAsaasPaymentId(payment.id);
        
        if (booking != null) {
            log.info("Pagamento confirmado para Reserva ID: {}", booking.getId());
            booking.setStatus("APPROVED");
            bookingRepository.save(booking);
            return true; // Era uma reserva
        }
        return false; // Não era reserva
    }

    /**
     * Cancela a reserva caso o pagamento falhe.
     */
    private void cancelarReserva(AsaasPaymentData payment) {
        Booking booking = bookingRepository.findByAsaasPaymentId(payment.id);
        if (booking != null) {
            log.warn("Pagamento falhou para Reserva ID: {}", booking.getId());
            booking.setStatus("REJECTED");
            bookingRepository.save(booking);
        }
    }

    /**
     * Processa comissão de afiliados se o pagamento for de uma mensalidade de condomínio.
     */
    private void processarComissaoAfiliado(AsaasPaymentData payment) {
        Tenant tenant = tenantRepository.findByAsaasCustomerId(payment.customer);
        
        if (tenant != null && tenant.getAfiliado() != null) {
            log.info("Gerando comissão para afiliado {} referente ao condomínio {}", 
                    tenant.getAfiliado().getId(), tenant.getNome());

            // Regra: 10% de Comissão
            BigDecimal valorPago = payment.value;
            BigDecimal valorComissao = valorPago.multiply(new BigDecimal("0.10"));

            Comissao comissao = Comissao.builder()
                    .afiliado(tenant.getAfiliado())
                    .condominioPagante(tenant)
                    .valor(valorComissao)
                    .dataVenda(LocalDate.now())
                    .dataLiberacao(LocalDate.now().plusDays(30)) // D+30
                    .status(StatusComissao.BLOQUEADO)
                    .asaasTransferId(payment.id) // Guarda ID para rastreio
                    .build();

            comissaoRepository.save(comissao);
        }
    }

    // --- DTOs INTERNOS PARA JSON ---
    @Data
    public static class AsaasWebhookEvent {
        public String event;
        public AsaasPaymentData payment;
    }

    @Data
    public static class AsaasPaymentData {
        public String id;
        public String customer;
        public BigDecimal value;
        public String billingType;
        public String description;
    }
}