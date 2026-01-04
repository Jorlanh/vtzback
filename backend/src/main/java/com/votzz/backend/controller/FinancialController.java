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

    // --- SALDO ---
    @GetMapping("/balance")
    public ResponseEntity<CondoFinancial> getBalance(@AuthenticationPrincipal User user) {
        if(user.getTenant() == null) return ResponseEntity.ok(new CondoFinancial());
        // Busca saldo vinculado ao tenant (ou o último global se não tiver filtro ainda)
        return ResponseEntity.ok(financialRepository.findFirstByOrderByLastUpdateDesc());
    }

    @PostMapping("/update")
    public ResponseEntity<CondoFinancial> updateBalance(@RequestBody Map<String, BigDecimal> payload, @AuthenticationPrincipal User user) {
        CondoFinancial fin = new CondoFinancial();
        fin.setBalance(payload.get("balance"));
        fin.setLastUpdate(LocalDateTime.now());
        fin.setUpdatedBy(user.getNome());
        return ResponseEntity.ok(financialRepository.save(fin));
    }

    // --- RELATÓRIOS (PDFs) ---

    @GetMapping("/reports")
    public ResponseEntity<List<ReportDTO>> listReports(@AuthenticationPrincipal User user) {
        if (user.getTenant() == null) return ResponseEntity.ok(List.of());

        List<FinancialReport> reports = reportRepository.findByTenantIdOrderByYearDescCreatedAtDesc(user.getTenant().getId());
        
        // Limita aos últimos 12 (opcional, mas boa prática)
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
            // CORREÇÃO AQUI: Chama o método 'uploadFile' do serviço S3 (não mais 'storeFile')
            String fileUrl = fileStorageService.uploadFile(file); 

            // 2. Salva no banco
            FinancialReport report = new FinancialReport();
            report.setTenant(user.getTenant());
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