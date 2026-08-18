package com.votzz.backend.controller;

import com.votzz.backend.domain.Poll;
import com.votzz.backend.repository.PollRepository;
import com.votzz.backend.service.PollReportService; // <--- IMPORTANTE: Importar o serviço correto
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/polls")
public class PollController {

    @Autowired
    private PollRepository pollRepository;

    // --- CORREÇÃO 1: Injetar PollReportService, não AuditService ---
    @Autowired
    private PollReportService pollReportService; 

    // --- CORREÇÃO 2: Alterar retorno para InputStreamResource (Melhor performance) ---
    @GetMapping("/{id}/audit")
    public ResponseEntity<InputStreamResource> downloadAudit(@PathVariable UUID id) {
        
        // 1. Apenas buscamos o título para o nome do arquivo
        Poll poll = pollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquete não encontrada"));

        // 2. Gerar o PDF usando o serviço correto (passando o ID)
        ByteArrayInputStream pdfStream = pollReportService.generatePollPdf(id);

        String filename = "auditoria_" + poll.getTitle().replaceAll("\\s+", "_") + ".pdf";

        // 3. Retornar como Stream
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}