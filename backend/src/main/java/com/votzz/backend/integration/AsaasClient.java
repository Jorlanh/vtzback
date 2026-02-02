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

    public String createCustomer(String name, String cpfCnpj, String email, String phoneRaw) {
        String cleanCpf = cpfCnpj != null ? cpfCnpj.replaceAll("\\D", "") : "";
        
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("cpfCnpj", cleanCpf);
        body.put("email", email);
        
        // Lógica Híbrida (Celular vs Fixo)
        if (phoneRaw != null && !phoneRaw.isBlank()) {
            String cleanPhone = phoneRaw.replaceAll("\\D", "");
            if (cleanPhone.length() > 10) { 
                body.put("mobilePhone", cleanPhone);
            } else {
                body.put("phone", cleanPhone);
            }
        }

        // DEBUG: Imprime o que está sendo enviado
        log.info("ASAAS DEBUG - Criando Cliente: URL={}/customers Payload={}", apiUrl, body);

        try {
            Map response = getClient().post()
                .uri(apiUrl + "/customers")
                .headers(h -> h.addAll(getHeaders()))
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("id")) {
                log.info("ASAAS SUCESSO - Cliente criado: {}", response.get("id"));
                return (String) response.get("id");
            }
        } catch (RestClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("ERRO API ASAAS ({}): {}", e.getStatusCode(), errorBody);
            
            // ESTRATÉGIA DE AUTO-RECUPERAÇÃO
            if (e.getStatusCode().value() == 400) {
                 log.info("Tentando recuperar cliente existente por CPF/CNPJ: {}", cleanCpf);
                 String existingId = fetchCustomerId(cleanCpf); // Tenta por CPF
                 
                 // Se não achou por CPF, tenta por Email (caso o erro seja email duplicado)
                 if (existingId == null) {
                     log.info("Não achou por CPF. Tentando recuperar por Email: {}", email);
                     existingId = fetchCustomerIdByEmail(email);
                 }

                 if (existingId != null) {
                     log.info("Cliente recuperado com sucesso: {}", existingId);
                     return existingId;
                 }
            }
            
            // Monta mensagem de erro amigável para o Frontend
            String msgErro = (errorBody != null && !errorBody.isBlank()) ? errorBody : "Erro HTTP " + e.getStatusCode() + " (" + e.getStatusText() + ")";
            throw new RuntimeException("Falha Asaas: " + msgErro);
        } catch (Exception e) {
            log.error("Erro Genérico Asaas createCustomer: {}", e.getMessage(), e);
            throw new RuntimeException("Falha interna de conexão com Asaas");
        }
        throw new RuntimeException("Falha desconhecida ao criar cliente Asaas (Sem ID)");
    }

    private String fetchCustomerId(String cpfCnpj) {
        try {
            Map response = getClient().get()
                .uri(apiUrl + "/customers?cpfCnpj=" + cpfCnpj)
                .headers(h -> h.addAll(getHeaders()))
                .retrieve()
                .body(Map.class);
            return extractIdFromResponse(response);
        } catch (Exception e) {
            log.warn("Falha ao buscar por CPF: {}", e.getMessage());
            return null;
        }
    }

    private String fetchCustomerIdByEmail(String email) {
        try {
            Map response = getClient().get()
                .uri(apiUrl + "/customers?email=" + email)
                .headers(h -> h.addAll(getHeaders()))
                .retrieve()
                .body(Map.class);
            return extractIdFromResponse(response);
        } catch (Exception e) {
            log.warn("Falha ao buscar por Email: {}", e.getMessage());
            return null;
        }
    }

    private String extractIdFromResponse(Map response) {
        if (response != null && response.containsKey("data")) {
            List<Map> data = (List<Map>) response.get("data");
            if (!data.isEmpty()) {
                return (String) data.get(0).get("id");
            }
        }
        return null;
    }

    // --- MÉTODOS DE COBRANÇA (MANTIDOS) ---

    public Map<String, Object> createSubscriptionCharge(String customerId, BigDecimal value, int maxInstallmentCount) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "UNDEFINED"); 
        body.put("value", value);
        body.put("dueDate", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE));
        body.put("description", "Assinatura Votzz - Plano Custom");
        body.put("maxInstallmentCount", maxInstallmentCount);

        return sendPaymentRequest(body);
    }

    public Map<String, Object> createPixCharge(String customerId, BigDecimal value, String walletIdAfiliado, BigDecimal percentualAfiliado) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("value", value);
        body.put("dueDate", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE));
        body.put("description", "Ativação Plano Custom Votzz");

        if (walletIdAfiliado != null && !walletIdAfiliado.isBlank() && percentualAfiliado != null && percentualAfiliado.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> splitRule = new HashMap<>();
            splitRule.put("walletId", walletIdAfiliado);
            splitRule.put("percent", percentualAfiliado);
            body.put("split", List.of(splitRule));
        }
        return sendPaymentRequest(body);
    }
    
    public Map<String, Object> createPixCharge(String customerId, BigDecimal value) {
        return createPixCharge(customerId, value, null, BigDecimal.ZERO);
    }

    public String criarCobrancaSplit(String customerId, BigDecimal valorTotal, String walletCondominio, BigDecimal taxaVotzz, String billingType) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", billingType != null ? billingType : "PIX");
        body.put("value", valorTotal);
        body.put("dueDate", LocalDate.now().plusDays(3).toString());
        body.put("description", "Reserva de Área Comum");

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

    private Map<String, Object> sendPaymentRequest(Map<String, Object> body) {
        try {
            Map payment = getClient().post()
                .uri(apiUrl + "/payments")
                .headers(h -> h.addAll(getHeaders()))
                .body(body)
                .retrieve()
                .body(Map.class);

            if (payment != null && payment.containsKey("id")) {
                Map<String, Object> result = new HashMap<>();
                result.put("paymentId", payment.get("id"));
                result.put("value", body.get("value"));
                
                if (payment.containsKey("invoiceUrl")) {
                    result.put("invoiceUrl", payment.get("invoiceUrl"));
                }
                else if ("PIX".equals(body.get("billingType"))) {
                     try {
                         Map qrCode = getClient().get()
                            .uri(apiUrl + "/payments/" + payment.get("id") + "/pixQrCode")
                            .headers(h -> h.addAll(getHeaders()))
                            .retrieve().body(Map.class);
                         if (qrCode != null) {
                             result.put("encodedImage", qrCode.get("encodedImage"));
                             result.put("payload", qrCode.get("payload"));
                         }
                     } catch(Exception e) {
                         log.warn("Falha ao buscar QR Code Pix: {}", e.getMessage());
                     }
                }
                return result;
            }
        } catch (RestClientResponseException e) {
            log.error("Erro Pagamento Asaas: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Erro Asaas Pagamento: " + e.getResponseBodyAsString());
        }
        throw new RuntimeException("Erro interno ao gerar cobrança.");
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
            return "ERR_TRANSFER"; 
        }
    }
}