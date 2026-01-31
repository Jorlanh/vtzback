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

    // CHAVE MESTRA (Sua conta Votzz - Usada para Planos e Afiliados)
    @Value("${asaas.api.key}")
    private String masterApiKey;

    private final RestClient.Builder restClientBuilder;

    private RestClient getClient() {
        return restClientBuilder.build();
    }

    // --- HELPER DE CABEÇALHOS INTELIGENTE ---
    // Se tenantKey for nulo, usa a masterApiKey.
    private HttpHeaders getHeaders(String tenantKey) {
        HttpHeaders headers = new HttpHeaders();
        // Lógica Híbrida: Prioriza a chave do condomínio, senão usa a Mestra
        String tokenToUse = (tenantKey != null && !tenantKey.isBlank()) ? tenantKey : masterApiKey;
        
        headers.set("access_token", tokenToUse);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Votzz-System");
        return headers;
    }

    // ==================================================================================
    // MÉTODOS PÚBLICOS (SOBRECARGA PARA MULTI-TENANT)
    // ==================================================================================

    // 1. CRIAR CLIENTE (CUSTOMER)

    // Padrão (Usa Mestra - Para Assinaturas da Plataforma)
    public String createCustomer(String name, String cpfCnpj, String email, String mobilePhone) {
        return createCustomer(name, cpfCnpj, email, mobilePhone, null);
    }

    // Multi-Tenant (Usa Chave do Condomínio - Para Reservas de Área)
    public String createCustomer(String name, String cpfCnpj, String email, String mobilePhone, String tenantApiKey) {
        String cleanCpf = cpfCnpj != null ? cpfCnpj.replaceAll("\\D", "") : "";
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("cpfCnpj", cleanCpf);
        body.put("email", email);
        
        if (mobilePhone != null && !mobilePhone.isBlank()) {
            body.put("mobilePhone", mobilePhone.replaceAll("\\D", ""));
        }

        try {
            Map response = getClient().post()
                .uri(apiUrl + "/customers")
                .headers(h -> h.addAll(getHeaders(tenantApiKey))) // <--- Usa a chave dinâmica
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
        } catch (RestClientResponseException e) {
            log.error("Erro API Asaas (Create Customer): Status={} Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getResponseBodyAsString().contains("already exists")) {
                 log.info("Cliente já existe no Asaas. Buscando ID original...");
                 return fetchCustomerIdByCpf(cleanCpf, tenantApiKey); // <--- Busca com a chave certa
            }
            throw new RuntimeException("Erro Asaas ao criar cliente: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro Genérico Asaas createCustomer: {}", e.getMessage());
            throw new RuntimeException("Falha de conexão com Asaas");
        }
        throw new RuntimeException("Falha ao criar cliente Asaas (Sem ID na resposta)");
    }

    // 2. BUSCAR CLIENTE POR CPF

    private String fetchCustomerIdByCpf(String cpfCnpj, String tenantApiKey) {
        try {
            Map response = getClient().get()
                .uri(apiUrl + "/customers?cpfCnpj=" + cpfCnpj)
                .headers(h -> h.addAll(getHeaders(tenantApiKey))) // <--- Usa a chave dinâmica
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map> data = (List<Map>) response.get("data");
                if (!data.isEmpty()) {
                    return (String) data.get(0).get("id");
                }
            }
        } catch (Exception e) {
            log.error("Erro ao buscar cliente existente: {}", e.getMessage());
        }
        throw new RuntimeException("Cliente existe no Asaas mas não foi possível recuperar o ID.");
    }

    // 3. CRIAR COBRANÇA PIX

    // Padrão (Usa Mestra - Para Assinaturas Votzz)
    public Map<String, Object> createPixCharge(String customerId, BigDecimal value) {
        return createPixCharge(customerId, value, null, BigDecimal.ZERO);
    }
    
    // Com Split (Usa Mestra - Para Afiliados)
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
        return sendPaymentRequest(body, null); // Passa null para usar a Mestra
    }

    // NOVO: Multi-Tenant (Usa Chave do Condomínio - Para Reservas)
    public Map<String, Object> createPixChargeForTenant(String customerId, BigDecimal value, String description, String tenantApiKey) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("value", value);
        // Vencimento curto (1 dia) para reservas de área comum
        body.put("dueDate", LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE)); 
        body.put("description", description);

        // NÂO TEM SPLIT AQUI (O dinheiro vai todo pro condomínio, pois usamos a chave dele)
        
        return sendPaymentRequest(body, tenantApiKey); // <--- Chave do Condomínio
    }

    // 4. MÉTODOS DE ASSINATURA (Usa Mestra - Mantidos inalterados)

    public Map<String, Object> createSubscriptionCharge(String customerId, BigDecimal value, int maxInstallmentCount) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "UNDEFINED"); 
        body.put("value", value);
        body.put("dueDate", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE));
        body.put("description", "Assinatura Votzz - Plano Custom");
        body.put("maxInstallmentCount", maxInstallmentCount);

        return sendPaymentRequest(body, null); // Mestra
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

        // Método antigo que retorna só String, mantido para compatibilidade, usando Mestra
        try {
            Map response = getClient().post()
                .uri(apiUrl + "/payments")
                .headers(h -> h.addAll(getHeaders(null))) // Mestra
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

    // 5. ENVIO GENÉRICO (CORAÇÃO DA MUDANÇA)

    // Agora aceita apiKey opcional
    private Map<String, Object> sendPaymentRequest(Map<String, Object> body, String specificApiKey) {
        try {
            Map payment = getClient().post()
                .uri(apiUrl + "/payments")
                .headers(h -> h.addAll(getHeaders(specificApiKey))) // <--- Injeta a chave correta
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
                            .headers(h -> h.addAll(getHeaders(specificApiKey))) // <--- Injeta a chave correta
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
            throw new RuntimeException("Erro Asaas: " + e.getResponseBodyAsString());
        }
        throw new RuntimeException("Erro interno ao gerar cobrança.");
    }

    // 6. TRANSFERÊNCIA (Para Afiliados - Usa Mestra)
    public String transferirPix(String chavePix, BigDecimal valor) {
        Map<String, Object> body = new HashMap<>();
        body.put("value", valor);
        body.put("operationType", "PIX");
        body.put("pixAddressKey", chavePix); 

        try {
            Map response = getClient().post()
                .uri(apiUrl + "/transfers")
                .headers(h -> h.addAll(getHeaders(null))) // Mestra
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