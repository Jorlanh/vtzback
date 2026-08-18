package com.votzz.backend.dto;
import java.util.List;

// Usando Record ou Class padrão
public record VoteRequest(
    String userId, 
    String optionId, 
    List<String> units // NOVO: Recebe lista de unidades selecionadas
) {}