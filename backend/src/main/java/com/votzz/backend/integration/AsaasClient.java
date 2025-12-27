package com.votzz.backend.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class AsaasClient {

    @Value("${asaas.api.url}") // Defina https://sandbox.asaas.com/api/v3 no properties
    private String apiUrl;

    @Value("${asaas.api.key}") // Defina sua chave $aact... no properties
    private String apiKey;

    @Value("${asaas.wallet.master-id}") // ID da sua carteira principal no Asaas
    private String masterWalletId;

    private final RestClient restClient;

    public AsaasClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /**
     * Cria uma cobrança real com Split no Asaas.
     */
    public String criarCobrancaSplit(String customerId, BigDecimal valorTotal, String walletCondominio, BigDecimal taxaVotzz) {
        // Cálculo: O Condomínio recebe (Total - Taxa), o Votzz recebe a Taxa
        BigDecimal valorLiquidoCondominio = valorTotal.subtract(taxaVotzz);

        // Configuração do Split real
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

        var body = Map.of(
            "customer", customerId,
            "billingType", "PIX",
            "value", valorTotal,
            "dueDate", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE),
            "split", splitConfig,
            "description", "Reserva de Área Comum - Votzz SaaS"
        );

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
            "scheduleDate", LocalDate.now().toString() // Paga hoje
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
            // Em produção, deve-se tratar erros como "Saldo Insuficiente"
            throw new RuntimeException("Erro na transferência PIX: " + e.getMessage());
        }
    }
}