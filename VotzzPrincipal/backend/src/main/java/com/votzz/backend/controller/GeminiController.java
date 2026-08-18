package com.votzz.backend.controller;

import com.votzz.backend.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GeminiController {

    private final GeminiService geminiService;

    @PostMapping("/ask")
    public Map<String, String> askSirius(@RequestBody Map<String, Object> payload) {
        String message = (String) payload.get("message");
        boolean isLoggedIn = (boolean) payload.getOrDefault("isLoggedIn", false);
        
        String role = isLoggedIn ? "Assistente de Elite Logado" : "Consultor de Vendas Votzz";
        String response = geminiService.getAiResponse(message, role);
        
        return Map.of("response", response);
    }

    @PostMapping("/generate-description")
    public Map<String, String> generateDescription(@RequestBody Map<String, String> payload) {
        String prompt = "Gere uma descrição profissional para uma assembleia sobre: " + payload.get("topic") + ". Detalhes: " + payload.get("details");
        return Map.of("response", geminiService.getAiResponse(prompt, "Secretário de Assembleia"));
    }

    @PostMapping("/generate-notification")
    public Map<String, String> generateNotification(@RequestBody Map<String, String> payload) {
        String prompt = "Gere um rascunho de notificação para a assembleia: " + payload.get("assemblyTitle") + " que encerra em: " + payload.get("endDate");
        return Map.of("response", geminiService.getAiResponse(prompt, "Gestor de Comunicação"));
    }

    @PostMapping("/analyze-sentiment")
    @SuppressWarnings("unchecked")
    public Map<String, String> analyzeSentiment(@RequestBody Map<String, Object> payload) {
        List<String> messages = (List<String>) payload.get("messages");
        // Nota: Garanta que este método existe no seu GeminiService
        return Map.of("response", geminiService.summarizeChat(messages));
    }
}