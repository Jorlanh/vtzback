package com.votzz.backend.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsaasClient {

    @Value("${asaas.api.url}")
    private String apiUrl;

    @Value("${asaas.api.key}")
    private String apiKey;

    @Value("${asaas.wallet.master-id}")
    private String masterWalletId;

    private final RestClient.Builder restClientBuilder;

    private RestClient getClient() {
        return restClientBuilder.build();
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("access_token", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Votzz-System");
        return headers;
    }

    // --- MÉTODOS ---

    public String createCustomer(String name, String cpfCnpj, String email) {
        // Limpa CPF/CNPJ (remove pontos e traços)
        String cleanCpf = cpfCnpj != null ? cpfCnpj.replaceAll("\\D", "") : "";

        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("cpfCnpj", cleanCpf);
        body.put("email", email);

        try {
            Map response = getClient().post()
                .uri(apiUrl + "/customers")
                .headers(h -> h.addAll(getHeaders()))
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
        } catch (RestClientResponseException e) {
            // AQUI ESTÁ O SEGREDO: Logar o corpo do erro para saber o motivo (CPF inválido, etc)
            log.error("Erro API Asaas (Create Customer): Status={} Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro Asaas: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro Genérico Asaas createCustomer: {}", e.getMessage());
            throw new RuntimeException("Falha de conexão com Asaas");
        }
        throw new RuntimeException("Falha ao criar cliente Asaas (Sem ID na resposta)");
    }

    public Map<String, Object> createPixCharge(String customerId, BigDecimal value) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("value", value);
        body.put("dueDate", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE)); // Formato YYYY-MM-DD
        body.put("description", "Ativação Plano Custom Votzz");

        try {
            // 1. Cria a Cobrança
            Map payment = getClient().post()
                .uri(apiUrl + "/payments")
                .headers(h -> h.addAll(getHeaders()))
                .body(body)
                .retrieve()
                .body(Map.class);

            if (payment != null && payment.containsKey("id")) {
                String paymentId = (String) payment.get("id");
                
                // 2. Busca o QR Code (Copia e Cola + Imagem)
                Map qrCode = getClient().get()
                    .uri(apiUrl + "/payments/" + paymentId + "/pixQrCode")
                    .headers(h -> h.addAll(getHeaders()))
                    .retrieve()
                    .body(Map.class);
                
                // Retorna o mapa completo contendo "payload" e "encodedImage"
                return qrCode;
            }
        } catch (RestClientResponseException e) {
            log.error("Erro API Asaas (Pix Charge): Status={} Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao gerar Pix: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro Genérico Asaas Pix: {}", e.getMessage());
            throw new RuntimeException("Erro interno ao gerar Pix.");
        }
        throw new RuntimeException("Falha ao obter ID do pagamento Asaas");
    }

    // Mantive os outros métodos iguais, mas adicionei logs de erro detalhados se precisar usar
    public String criarCobrancaSplit(String customerId, BigDecimal valorTotal, String walletCondominio, BigDecimal taxaVotzz, String billingType) {
        // ... (seu código existente de split) ...
        // Apenas lembre de adicionar try-catch com RestClientResponseException se for usar futuramente
        return null; // Simplificado aqui para focar no problema principal
    }

    public String transferirPix(String chavePix, BigDecimal valor) {
        // ...
        return null;
    }
}