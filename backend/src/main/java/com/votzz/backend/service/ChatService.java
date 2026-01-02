package com.votzz.backend.service;

import com.votzz.backend.domain.ChatMessage;
import com.votzz.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Font;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatRepository;
    private final RestTemplate restTemplate = new RestTemplate(); // Para chamadas HTTP
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public byte[] gerarResumoAssembleia(UUID assembleiaId) {
        // 1. Busca as mensagens do banco
        List<ChatMessage> mensagens = chatRepository.findByAssemblyIdOrderByCreatedAtAsc(assembleiaId);
        
        if (mensagens.isEmpty()) {
            return gerarPdf("Nenhuma mensagem registrada nesta assembleia para gerar resumo.");
        }

        // 2. Monta o Prompt para a IA
        StringBuilder prompt = new StringBuilder("Atue como um secretário de assembleia de condomínio experiente. ");
        prompt.append("Abaixo está o log do chat em tempo real. Resuma os pontos principais, ");
        prompt.append("decisões tomadas e eventuais conflitos em tópicos claros e profissionais.\n\n");
        prompt.append("LOG DO CHAT:\n");
        
        mensagens.forEach(m -> prompt
            .append("[").append(m.getUserName()).append("]: ") 
            .append(m.getContent()).append("\n")); 

        // 3. Chama a API Real do Google Gemini
        String resumoTexto = chamarGeminiApi(prompt.toString());

        // 4. Converte o texto da IA em um PDF oficial
        return gerarPdf(resumoTexto);
    }

    private String chamarGeminiApi(String promptTexto) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

        // Estrutura de JSON que o Gemini exige
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", promptTexto)
                ))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            // Navega no JSON de resposta: candidates[0].content.parts[0].text
            List candidates = (List) response.getBody().get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);
            
            return (String) firstPart.get("text");
        } catch (Exception e) {
            return "Erro ao processar resumo com IA: " + e.getMessage();
        }
    }

    private byte[] gerarPdf(String texto) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            
            // Cabeçalho do PDF
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            document.add(new Paragraph("VOTZZ - RELATÓRIO DE AUDITORIA E RESUMO IA", titleFont));
            document.add(new Paragraph(" ")); // Linha em branco
            
            // Corpo do texto
            document.add(new Paragraph(texto));
            
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }
}