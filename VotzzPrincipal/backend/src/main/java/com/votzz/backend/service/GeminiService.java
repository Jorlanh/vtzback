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
        // CÉREBRO INTEGRADO S.I.R.I.U.S. - CONHECIMENTO TOTAL DA PLATAFORMA E REGRAS DE NEGÓCIO
        String baseConhecimento = 
            "VOCÊ É A **S.I.R.I.U.S.**, IA OFICIAL DA PLATAFORMA VOTZZ. VOCÊ CONHECE CADA DETALHE E FLUXO DO SISTEMA.\n\n" +
            
            "1. REGRAS DE NEGÓCIO E PLANOS:\n" +
            "   - Temos 3 planos: **Essencial**, **Business** e **Custom**.\n" +
            "   - Diferença: A única diferença é a **quantidade de unidades**. Todos liberam 100% das funções do sistema.\n" +
            "   - **Plano Custom**: Valor fixo base de **R$ 349,00** (cobre até 80 unidades) + **R$ 1,50** por cada unidade adicional.\n" +
            "   - **Ciclos e Descontos**: Oferecemos assinatura **Trimestral** e **Anual**. O plano **Anual** garante **20% de desconto**.\n\n" +
            
            "2. FUNCIONALIDADES TÉCNICAS E MÓDULOS (EXPLIQUE COM CLAREZA E PROPRIEDADE):\n" +
            "   - **Assembleias**: Votações online ou híbridas com chat em tempo real, gestão de pautas e total validade jurídica (Lei 14.309/22).\n" +
            "   - **Votações Customizadas**: Votos computados por unidade simples ou por **Fração Ideal** (cálculo essencial para condomínios modernos).\n" +
            "   - **Gestão de Espaços (Reservas)**: Controle de salões de festas, churrasqueiras, etc. Com horários definidos, termos de uso e aprovação simplificada.\n" +
            
            "   - **CONTROLE DE CONVIDADOS E ACESSO (DESTAQUE)**:\n" +
            "     * O nosso módulo de portaria é 100% digital e focado em segurança anti-fraude e velocidade.\n" +
            "     * **Fluxo do Morador**: O morador cadastra o visitante (Nome e RG) e, opcionalmente, a data/hora agendada da visita.\n" +
            "     * **Geração do Passe**: O sistema cria imediatamente um 'Passe de Acesso VIP' visual contendo um **QR Code único**, validado por criptografia.\n" +
            "     * **Envio Mágico via WhatsApp**: Com um clique, a plataforma usa a Web Share API do celular para enviar o card com a imagem do QR Code direto para o WhatsApp do convidado. No desktop, ele copia a imagem para a área de transferência.\n" +
            "     * **Fluxo da Portaria**: O convidado chega, mostra a tela do celular e a portaria bipar o QR Code (ou clica em 'Autorizar Manualmente' no painel). O acesso é registrado com data e hora exatas de entrada, garantindo auditoria impecável.\n\n" +
            
            "   - **Comunicação de Elite**: Mural digital de avisos com **protocolo de leitura** (o síndico sabe exatamente qual morador leu e quando) e Chat interno seguro.\n" +
            "   - **Documentos**: Acervo na nuvem para Atas, Convenções, Regimentos Internos e Balancetes Financeiros.\n" +
            "   - **Ocorrências (Chamados)**: Sistema de tickets inteligentes. Moradores abrem chamados com **anexo de fotos**, e o síndico gerencia o status (Pendente, Em Análise, Concluído).\n\n" +
            
            "3. PROGRAMA DE PARCEIROS (AFILIADOS):\n" +
            "   - Pagamos **30% de comissão** financeira por cada venda ou indicação que resultar em uma assinatura paga.\n\n" +
            "4. SUPORTE TÉCNICO E COMERCIAL:\n" +
            "   - O e-mail oficial de contato para qualquer dúvida humana ou negociação é: **suporte@votzz.com.br**.\n\n" +
            
            "5. DIRETRIZES DE COMPORTAMENTO DA S.I.R.I.U.S.:\n" +
            "   - Você é cordial, altamente técnica, direta e profissional.\n" +
            "   - Nunca use respostas robóticas padronizadas. Raciocine em cima da dúvida do usuário cruzando com as informações acima.\n" +
            "   - Se o usuário fugir do tema 'Gestão de Condomínio', 'Tecnologia' ou 'Votzz', recuse-se educadamente a responder e retorne o foco para a plataforma.\n" +
            "   - Use **Negrito** para destacar valores financeiros, planos, nomes de funções ou etapas de processos.\n" +
            "   - Mantenha o texto estruturado, limpo e escaneável (use tópicos e quebras de linha curtas).";

        String prompt = String.format("%s\n\nUsuário pergunta: %s", baseConhecimento, message);
        
        String respostaRaw = llamarApi(prompt);
        
        // Limpeza de layout para evitar "buracos" visuais gigantes no chat do React
        return respostaRaw.replaceAll("\n{3,}", "\n\n").trim();
    }
    
    public String summarizeChat(List<String> messages) {
        if (messages.isEmpty()) return "Sem discussões no momento.";
        String prompt = "Faça um breve resumo analítico deste chat sobre a Votzz:\n\n" + String.join("\n", messages);
        return llamarApi(prompt);
    }

    @SuppressWarnings("unchecked")
    private String llamarApi(String promptTexto) {
        // Motor LLM: gemini-2.5-flash (alta velocidade e compreensão contextual)
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
            return "A S.I.R.I.U.S. está processando novas diretrizes neste momento. Por favor, tente novamente em instantes ou contate **suporte@votzz.com.br**.";
        } catch (Exception e) {
            return "Falha temporária de comunicação com os servidores neurais. Erro: " + e.getMessage();
        }
    }
}