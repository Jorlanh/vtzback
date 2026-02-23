package com.votzz.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public String getAiResponse(String message, String role) {
        // CÉREBRO INTEGRADO S.I.R.I.U.S. - CONHECIMENTO TOTAL DA PLATAFORMA
        String baseConhecimento = 
            "VOCÊ É A **S.I.R.I.U.S.**, IA OFICIAL DA VOTZZ. VOCÊ CONHECE CADA DETALHE DA PLATAFORMA.\n\n" +
            "1. REGRAS DE NEGÓCIO E PLANOS:\n" +
            "   - Temos 3 planos: **Essencial**, **Business** e **Custom**.\n" +
            "   - Diferença: A única diferença é a **quantidade de unidades**. Todos liberam 100% das funções.\n" +
            "   - **Plano Custom**: Valor fixo de **R$ 349,00** (pelas 80 unidades) + **R$ 1,50** por cada unidade adicional.\n" +
            "   - **Ciclos e Descontos**: Oferecemos planos **Trimestral** e **Anual**. O plano **Anual** tem **20% de desconto**.\n\n" +
            "2. FUNCIONALIDADES TÉCNICAS (EXPLIQUE COM PROPRIEDADE):\n" +
            "   - **Assembleias**: Online/Híbridas com chat, pautas e validade jurídica (Lei 14.309/22).\n" +
            "   - **Votações**: Por unidade ou por **Fração Ideal** (essencial para condomínios).\n" +
            "   - **Gestão de Espaços**: Reservas de áreas comuns com termos de uso e horários.\n" +
            "   - **Comunicação de Elite**: Mural com **protocolo de leitura** (o síndico vê quem visualizou) e Chat interno.\n" +
            "   - **Documentos**: Acervo digital de Atas, Convenções, Regimentos e Balancetes.\n" +
            "   - **Ocorrências**: Sistema de tickets com **anexo de fotos** e status de progresso.\n\n" +
            "3. AFILIADOS: Pagamos **30% de comissão** por cada indicação ativa.\n\n" +
            "4. SUPORTE E CONTATO: E-mail oficial: **suporte@votzz.com.br**.\n\n" +
            "5. COMPORTAMENTO DA IA:\n" +
            "   - Não use respostas prontas. Raciocine sobre a dúvida do usuário usando os dados acima.\n" +
            "   - Se o usuário fugir do tema 'Gestão de Condomínio' ou 'Votzz', retorne o foco para a plataforma.\n" +
            "   - Use **Negrito** para destacar valores, nomes de funções e e-mails.\n" +
            "   - Mantenha o texto escaneável com tópicos e quebras de linha.";

        String prompt = String.format("%s\n\nUsuário pergunta: %s", baseConhecimento, message);
        
        String respostaRaw = llamarApi(prompt);
        
        // Limpeza para evitar "buracos" no chat do React
        return respostaRaw.replaceAll("\n{3,}", "\n\n").trim();
    }
    
    public String summarizeChat(List<String> messages) {
        if (messages.isEmpty()) return "Sem discussões no momento.";
        String prompt = "Resuma o chat sobre a Votzz:\n\n" + String.join("\n", messages);
        return llamarApi(prompt);
    }

    @SuppressWarnings("unchecked")
    private String llamarApi(String promptTexto) {
        // Usando gemini-2.0-flash para maior velocidade e inteligência de raciocínio
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey.trim();

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", promptTexto)))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
            return "A S.I.R.I.U.S. está processando novas diretrizes. Por favor, tente novamente ou contate **suporte@votzz.com.br**.";
        } catch (Exception e) {
            return "Erro técnico: " + e.getMessage();
        }
    }
}