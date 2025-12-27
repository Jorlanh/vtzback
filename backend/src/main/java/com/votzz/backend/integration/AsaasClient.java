package com.votzz.backend.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap; // Import necessário para o Map mutável
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
     * Cria uma cobrança real com Split no Asaas.
     */
    public String criarCobrancaSplit(String customerId, BigDecimal valorTotal, String walletCondominio, BigDecimal taxaVotzz) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("value", valorTotal);
        body.put("dueDate", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE));
        body.put("description", "Reserva de Área Comum - Votzz SaaS");

        // LÓGICA DE SPLIT CONDICIONAL
        if (taxaVotzz.compareTo(BigDecimal.ZERO) > 0) {
            // Cenário Trimestral: Divide o valor (Condomínio recebe líquido, Votzz recebe taxa)
            BigDecimal valorLiquidoCondominio = valorTotal.subtract(taxaVotzz);
            
            var splitConfig = List.of(
                Map.of("walletId", walletCondominio, "fixedValue", valorLiquidoCondominio),
                Map.of("walletId", masterWalletId, "fixedValue", taxaVotzz)
            );
            body.put("split", splitConfig);
        } else {
            // Cenário Anual (Isento de taxa Votzz): 
            // Manda 100% para o condomínio. O Asaas cobra a tarifa bancária deles automaticamente.
            var splitConfig = List.of(
                Map.of("walletId", walletCondominio, "percentualValue", 100)
            );
            body.put("split", splitConfig);
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
            throw new RuntimeException("Resposta inválida do Asaas ao criar cobrança.");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao comunicar com Asaas: " + e.getMessage());
        }
    }

    /**
     * Realiza transferência PIX real para a conta do afiliado.
     */
    public String transferirPix(String chavePix, BigDecimal valor) {
        var body = Map.of(
            "value", valor,
            "operationType", "PIX",
            "pixAddressKey", chavePix,
            "description", "Pagamento de Comissão Votzz",
            "scheduleDate", LocalDate.now().toString()
        );

        try {
            Map response = restClient.post()
                .uri(apiUrl + "/transfers")
                .header("access_token", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

            return (String) response.get("id");
        } catch (Exception e) {
            throw new RuntimeException("Erro na transferência PIX: " + e.getMessage());
        }
    }
}