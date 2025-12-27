package com.votzz.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String getAiResponse(String message, String role) {
        // Simulação baseada no papel solicitado
        if (role.equalsIgnoreCase("Síndico")) {
            return "[IA Síndico]: Olá, recebi seu comentário. Vamos levar essa pauta para a próxima reunião técnica.";
        }
        return "[IA Morador]: Concordo plenamente! É fundamental priorizar a segurança e a valorização do nosso condomínio.";
    }

    public String summarizeChat(List<String> messages) {
        if (messages.isEmpty()) return "Sem discussões no momento.";
        return "Resumo IA: Os moradores demonstraram interesse no cronograma da obra e concordam com a taxa de rateio.";
    }
}