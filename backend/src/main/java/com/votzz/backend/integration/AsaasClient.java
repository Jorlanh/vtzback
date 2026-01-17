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
            log.error("Erro API Asaas (Create Customer): Status={} Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getResponseBodyAsString().contains("already exists")) {
                 log.warn("Cliente já existe, considere buscar antes de criar.");
                 // Em produção, buscar ID via GET /customers
                 return "cus_EXISTING_MOCK"; 
            }
            throw new RuntimeException("Erro Asaas: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro Genérico Asaas createCustomer: {}", e.getMessage());
            throw new RuntimeException("Falha de conexão com Asaas");
        }
        throw new RuntimeException("Falha ao criar cliente Asaas (Sem ID na resposta)");
    }

    // Método para ativação de plano (Split Percentual)
    public Map<String, Object> createPixCharge(String customerId, BigDecimal value, String walletIdAfiliado, BigDecimal percentualAfiliado) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("value", value);
        body.put("dueDate", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE));
        body.put("description", "Ativação Plano Custom Votzz");

        if (walletIdAfiliado != null && !walletIdAfiliado.isBlank() && percentualAfiliado.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> splitRule = new HashMap<>();
            splitRule.put("walletId", walletIdAfiliado);
            splitRule.put("percent", percentualAfiliado);
            body.put("split", List.of(splitRule));
        }

        return sendPaymentRequest(body);
    }
    
    // Sobrecarga para compatibilidade (sem split)
    public Map<String, Object> createPixCharge(String customerId, BigDecimal value) {
        return createPixCharge(customerId, value, null, BigDecimal.ZERO);
    }

    // --- NOVO MÉTODO: Cobrança de Reserva com Split Fixo ---
    // Usado pelo FacilitiesController e ReservationService
    public String criarCobrancaSplit(String customerId, BigDecimal valorTotal, String walletCondominio, BigDecimal taxaVotzz, String billingType) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", billingType != null ? billingType : "PIX");
        body.put("value", valorTotal);
        body.put("dueDate", LocalDate.now().plusDays(3).toString());
        body.put("description", "Reserva de Área Comum");

        // Regra de Split: 
        // Se houver wallet do condomínio, mandamos o valor total MENOS a taxa da Votzz para ele.
        // O restante (taxaVotzz) fica na conta mestre automaticamente.
        if (walletCondominio != null && !walletCondominio.isBlank()) {
            BigDecimal valorLiquidoCondominio = valorTotal.subtract(taxaVotzz);
            if (valorLiquidoCondominio.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> splitRule = new HashMap<>();
                splitRule.put("walletId", walletCondominio);
                splitRule.put("fixedValue", valorLiquidoCondominio);
                
                body.put("split", List.of(splitRule));
            }
        }

        try {
            Map response = getClient().post()
                .uri(apiUrl + "/payments")
                .headers(h -> h.addAll(getHeaders()))
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
        } catch (RestClientResponseException e) {
            log.error("Erro API Asaas (Split Charge): {}", e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao criar cobrança split: " + e.getResponseBodyAsString());
        }
        throw new RuntimeException("Falha ao criar cobrança split");
    }

    // Método auxiliar para evitar duplicação
    private Map<String, Object> sendPaymentRequest(Map<String, Object> body) {
        try {
            Map payment = getClient().post()
                .uri(apiUrl + "/payments")
                .headers(h -> h.addAll(getHeaders()))
                .body(body)
                .retrieve()
                .body(Map.class);

            if (payment != null && payment.containsKey("id")) {
                String paymentId = (String) payment.get("id");
                Map qrCode = getClient().get()
                    .uri(apiUrl + "/payments/" + paymentId + "/pixQrCode")
                    .headers(h -> h.addAll(getHeaders()))
                    .retrieve()
                    .body(Map.class);
                
                if (qrCode != null) {
                    qrCode.put("paymentId", paymentId);
                }
                return qrCode;
            }
        } catch (RestClientResponseException e) {
            log.error("Erro Pix Asaas: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao gerar Pix: " + e.getResponseBodyAsString());
        }
        throw new RuntimeException("Erro interno ao gerar Pix.");
    }

    public String transferirPix(String chavePix, BigDecimal valor) {
        Map<String, Object> body = new HashMap<>();
        body.put("value", valor);
        body.put("operationType", "PIX");
        body.put("pixAddressKey", chavePix); 

        try {
            Map response = getClient().post()
                .uri(apiUrl + "/transfers")
                .headers(h -> h.addAll(getHeaders()))
                .body(body)
                .retrieve()
                .body(Map.class);
            return (String) response.get("id");
        } catch (RestClientResponseException e) {
            log.error("Erro Transferência Pix: {}", e.getResponseBodyAsString());
            return "ERR_TRANSFER"; // Ou lançar exceção
        }
    }
}