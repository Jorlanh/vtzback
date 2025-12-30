package com.votzz.backend.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AsaasClient {

    @Value("${asaas.api.url}")
    private String apiUrl;

    @Value("${asaas.api.key}")
    private String apiKey;

    @Value("${asaas.wallet.master-id}")
    private String masterWalletId;

    private final RestClient restClient;

    public AsaasClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /**
     * Cria cobrança com Split suportando apenas PIX e BOLETO.
     */
    public String criarCobrancaSplit(
            String customerId, 
            BigDecimal valorTotal, 
            String walletCondominio, 
            BigDecimal taxaVotzz,
            String billingType // PIX ou BOLETO
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", billingType != null ? billingType : "PIX");
        body.put("value", valorTotal);
        body.put("dueDate", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE));
        body.put("description", "Reserva de Área Comum - Votzz SaaS");

        // --- LÓGICA DE SPLIT ---
        if (taxaVotzz.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal valorLiquidoCondominio = valorTotal.subtract(taxaVotzz);
            var splitConfig = List.of(
                Map.of("walletId", walletCondominio, "fixedValue", valorLiquidoCondominio),
                Map.of("walletId", masterWalletId, "fixedValue", taxaVotzz)
            );
            body.put("split", splitConfig);
        } else {
            // Plano Anual (Sem taxa Votzz): 100% para o condomínio
            body.put("split", List.of(
                Map.of("walletId", walletCondominio, "percentualValue", 100)
            ));
        }

        try {
            Map response = restClient.post()
                .uri(apiUrl + "/payments")
                .header("access_token", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
            throw new RuntimeException("Resposta inválida do Asaas.");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar pagamento no Asaas: " + e.getMessage());
        }
    }

    public String transferirPix(String chavePix, BigDecimal valor) {
        var body = Map.of(
            "value", valor, "operationType", "PIX", "pixAddressKey", chavePix,
            "description", "Comissão Votzz", "scheduleDate", LocalDate.now().toString()
        );
        
        try {
            Map response = restClient.post()
                .uri(apiUrl + "/transfers")
                .header("access_token", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
                
            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
            return null;
        } catch (Exception e) {
            // Logar erro mas não quebrar fluxo principal se possível
            System.err.println("Erro na transferência PIX: " + e.getMessage());
            return null;
        }
    }
}