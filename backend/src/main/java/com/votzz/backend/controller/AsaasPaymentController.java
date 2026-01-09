package com.votzz.backend.controller;

import com.votzz.backend.integration.AsaasClient;
import com.votzz.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal; // IMPORT NECESSÁRIO PARA CORRIGIR O ERRO
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class AsaasPaymentController {

    private final AsaasClient asaasClient;
    private final PaymentService paymentService;

    @PostMapping("/create-custom")
    public ResponseEntity<Map<String, Object>> createCustomCharge(@RequestBody Map<String, Object> request) {
        // 1. Validação de Input (Anti-Hacker)
        // Convertendo para Integer de forma segura para evitar ClassCastException
        int units = Integer.parseInt(request.get("units").toString());
        String frequency = (String) request.get("frequency");
        
        if (units <= 80) {
            return ResponseEntity.badRequest().build(); // Custom deve ser > 80
        }

        // 2. Calcula valor no SERVER-SIDE
        BigDecimal valorCalculado = paymentService.calculateCustomPlanPrice(units, frequency);

        // 3. Integração com Asaas
        // Substitua "ID_DO_CLIENTE" pela lógica de buscar o AsaasCustomerId do usuário logado
        Map<String, Object> asaasResponse = asaasClient.createPixCharge("ID_DO_CLIENTE", valorCalculado);
        
        return ResponseEntity.ok(asaasResponse);
    }
}