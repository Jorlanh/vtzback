package com.votzz.backend.controller;

import com.votzz.backend.domain.CondoFinancial;
import com.votzz.backend.domain.FinancialReport;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.CondoFinancialRepository;
import com.votzz.backend.repository.FinancialReportRepository;
import com.votzz.backend.service.FileStorageService; 
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FinancialController {

    private final CondoFinancialRepository financialRepository;
    private final FinancialReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    // --- SALDO (CORRIGIDO VAZAMENTO) ---
    @GetMapping("/balance")
    public ResponseEntity<CondoFinancial> getBalance(@AuthenticationPrincipal User user) {
        if(user.getTenant() == null) {
            // Retorna objeto vazio se não tiver tenant, mas com 200 OK
            return ResponseEntity.ok(new CondoFinancial());
        }
        
        // CORREÇÃO CRÍTICA: Busca APENAS pelo ID do Tenant do usuário logado
        return financialRepository.findByTenantId(user.getTenant().getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new CondoFinancial())); // Retorna vazio se ainda não tiver registro, mas não vaza o global
    }

    @PostMapping("/update")
    @Transactional
    public ResponseEntity<CondoFinancial> updateBalance(@RequestBody Map<String, BigDecimal> payload, @AuthenticationPrincipal User user) {
        if (user.getTenant() == null) return ResponseEntity.status(403).build();

        // Busca o registro existente do tenant OU cria um novo vinculado a ele
        CondoFinancial fin = financialRepository.findByTenantId(user.getTenant().getId())
                .orElse(new CondoFinancial());
        
        // Garante o vínculo correto com o Tenant
        if (fin.getTenant() == null) {
            fin.setTenant(user.getTenant());
        }

        fin.setBalance(payload.get("balance"));
        fin.setLastUpdate(LocalDateTime.now());
        fin.setUpdatedBy(user.getNome());
        
        return ResponseEntity.ok(financialRepository.save(fin));
    }

    // --- RELATÓRIOS (PDFs) ---

    @GetMapping("/reports")
    public ResponseEntity<List<ReportDTO>> listReports(@AuthenticationPrincipal User user) {
        if (user.getTenant() == null) return ResponseEntity.ok(List.of());

        // Garante filtro por Tenant ID
        List<FinancialReport> reports = reportRepository.findByTenantIdOrderByYearDescCreatedAtDesc(user.getTenant().getId());
        
        List<ReportDTO> dtos = reports.stream()
            .limit(12)
            .map(r -> new ReportDTO(r.getId().toString(), r.getMonth(), r.getYear(), r.getFileName(), r.getUrl()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/reports/upload")
    public ResponseEntity<?> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("month") String month,
            @RequestParam("year") int year,
            @AuthenticationPrincipal User user
    ) {
        if (user.getTenant() == null) return ResponseEntity.badRequest().body("Usuário sem condomínio.");

        try {
            String fileUrl = fileStorageService.uploadFile(file); 

            FinancialReport report = new FinancialReport();
            report.setTenant(user.getTenant()); // Vincula ao tenant correto
            report.setMonth(month);
            report.setYear(year);
            report.setFileName(file.getOriginalFilename());
            report.setUrl(fileUrl);
            
            reportRepository.save(report);

            return ResponseEntity.ok("Relatório salvo com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao salvar relatório: " + e.getMessage());
        }
    }

    public record ReportDTO(String id, String month, int year, String fileName, String url) {}
}