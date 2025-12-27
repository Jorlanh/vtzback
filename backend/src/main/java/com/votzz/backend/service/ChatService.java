package com.votzz.backend.service;

import com.votzz.backend.domain.ChatMessage;
import com.votzz.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatRepository;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public byte[] gerarResumoAssembleia(UUID assembleiaId) {
        // CORREÇÃO: Usar o novo nome do método (OrderByCreatedAtAsc)
        List<ChatMessage> mensagens = chatRepository.findByAssemblyIdOrderByCreatedAtAsc(assembleiaId);
        
        StringBuilder contexto = new StringBuilder("Analise o seguinte chat de assembleia de condomínio:\n");
        mensagens.forEach(m -> contexto
            .append(m.getUserName()).append(": ") 
            .append(m.getContent()).append("\n")); 
        
        contexto.append("\nInstrução: Resuma as principais decisões e conflitos desta assembleia em tópicos.");

        String resumoTexto = chamarGeminiApi(contexto.toString());

        return gerarPdf(resumoTexto);
    }

    private String chamarGeminiApi(String prompt) {
        // Implementação real da chamada ao Gemini viria aqui
        return "Resumo Simulado: A assembleia aprovou a reforma da piscina.";
    }

    private byte[] gerarPdf(String texto) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(texto));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }
}