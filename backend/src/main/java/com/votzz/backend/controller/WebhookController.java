package com.votzz.backend.controller;

import com.votzz.backend.domain.Comissao;
import com.votzz.backend.domain.StatusComissao;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.repository.ComissaoRepository;
import com.votzz.backend.repository.TenantRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final TenantRepository tenantRepository;
    private final ComissaoRepository comissaoRepository;

    @PostMapping("/asaas")
    public ResponseEntity<String> handleAsaasWebhook(@RequestBody AsaasWebhookEvent event, 
                                                     @RequestHeader(value = "asaas-access-token", required = false) String token) {
        // TODO: Validar o token de segurança do webhook do Asaas aqui

        if ("PAYMENT_RECEIVED".equals(event.event)) {
            processarPagamentoConfirmado(event.payment);
        }

        return ResponseEntity.ok("Received");
    }

    private void processarPagamentoConfirmado(AsaasPaymentData payment) {
        // 1. Achar o condomínio pelo ID de cliente do Asaas
        Tenant tenant = tenantRepository.findByAsaasCustomerId(payment.customer);
        
        if (tenant != null && tenant.getAfiliado() != null) {
            log.info("Gerando comissão para afiliado {} referente ao condomínio {}", 
                    tenant.getAfiliado().getId(), tenant.getNome());

            // 2. Calcular Comissão (10%)
            BigDecimal valorPago = payment.value;
            BigDecimal valorComissao = valorPago.multiply(new BigDecimal("0.10"));

            // 3. Criar registro
            Comissao comissao = Comissao.builder()
                    .afiliado(tenant.getAfiliado())
                    .condominioPagante(tenant)
                    .valor(valorComissao)
                    .dataVenda(LocalDate.now())
                    .dataLiberacao(LocalDate.now().plusDays(30)) // Regra D+30
                    .status(StatusComissao.BLOQUEADO)
                    .build();

            comissaoRepository.save(comissao);
        }
    }

    // DTOs internos para mapear o JSON do Asaas
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
    }
}