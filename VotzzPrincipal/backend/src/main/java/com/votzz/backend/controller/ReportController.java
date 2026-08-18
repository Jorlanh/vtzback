package com.votzz.backend.controller;

import com.votzz.backend.service.ReportService; // Import do seu serviço
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Importa RestController, GetMapping, etc.

import java.io.ByteArrayInputStream;
import java.util.UUID; // Importa UUID

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired 
    private ReportService reportService;

    @GetMapping("/assembly/{id}/csv")
    public ResponseEntity<InputStreamResource> downloadAudit(@PathVariable UUID id) {
        ByteArrayInputStream stream = reportService.generateAuditCsv(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auditoria.csv")
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(new InputStreamResource(stream));
    }
}