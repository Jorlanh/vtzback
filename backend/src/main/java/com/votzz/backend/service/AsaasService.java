package com.votzz.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class AsaasService {

    @Value("${asaas.api.url}") // Certifique-se de ter isso no application.properties
    private String apiUrl;

    @Value("${asaas.api.key}")
    private String apiKey;

    @Value("${asaas.wallet.master-id}")
    private String masterWalletId;

    private final RestClient restClient;

    // Injeção de dependência do RestClient Builder
    public AsaasService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /**
     * Cria uma cobrança REAL no Asaas com Split de pagamento.
     */
    public String criarCobrancaSplit(BigDecimal valorTotal, String payerId, String walletCondominio, BigDecimal taxaVotzz) {
        
        // O Condomínio recebe: Total - Taxa do Votzz
        BigDecimal valorLiquidoCondominio = valorTotal.subtract(taxaVotzz);

        // 1. Configuração do Split (Divisão do dinheiro)
        var splitConfig = List.of(
            Map.of(
                "walletId", walletCondominio,
                "fixedValue", valorLiquidoCondominio
            ),
            Map.of(
                "walletId", masterWalletId,
                "fixedValue", taxaVotzz
            )
        );

        // 2. Montagem do Payload JSON
        var body = Map.of(
            "customer", payerId,
            "billingType", "PIX",
            "value", valorTotal,
            "dueDate", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE),
            "split", splitConfig,
            "description", "Cobrança Votzz"
        );

        try {
            // 3. Chamada HTTP POST
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
            e.printStackTrace();
            throw new RuntimeException("Erro ao criar cobrança no Asaas: " + e.getMessage());
        }
    }

    /**
     * Realiza transferência PIX REAL para afiliados.
     */
    public void transferirPix(String chavePix, BigDecimal valor, String descricao) {
        var body = Map.of(
            "value", valor,
            "operationType", "PIX",
            "pixAddressKey", chavePix,
            "description", descricao,
            "scheduleDate", LocalDate.now().toString() // Paga hoje
        );

        try {
            restClient.post()
                .uri(apiUrl + "/transfers")
                .header("access_token", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity(); // Não precisamos do corpo da resposta, só saber se foi 200 OK
                
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao realizar transferência PIX: " + e.getMessage());
        }
    }
}