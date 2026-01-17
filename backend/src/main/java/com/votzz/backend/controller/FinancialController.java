package com.votzz.backend.controller;

import com.votzz.backend.core.tenant.TenantContext;
import com.votzz.backend.domain.CondoFinancial;
import com.votzz.backend.domain.FinancialReport;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.CondoFinancialRepository;
import com.votzz.backend.repository.FinancialReportRepository;
import com.votzz.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        UUID tenantId = TenantContext.getCurrentTenant();
        
        if (tenantId == null) {
            // Se por algum motivo o contexto não foi setado, usa o tenant do usuário como fallback
            if (user.getTenant() == null) {
                return ResponseEntity.ok(new CondoFinancial());
            }
            tenantId = user.getTenant().getId();
        }

        return financialRepository.findByTenantId(tenantId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new CondoFinancial()));
    }

    @PostMapping("/update")
    @Transactional
    public ResponseEntity<CondoFinancial> updateBalance(
            @RequestBody Map<String, BigDecimal> payload,
            @AuthenticationPrincipal User user) {

        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            if (user.getTenant() == null) {
                return ResponseEntity.status(403).body(null);
            }
            tenantId = user.getTenant().getId();
        }

        CondoFinancial fin = financialRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    CondoFinancial newFin = new CondoFinancial();
                    newFin.setTenant(user.getTenant()); // só cria se não existir
                    return newFin;
                });

        fin.setBalance(payload.get("balance"));
        fin.setLastUpdate(LocalDateTime.now());
        fin.setUpdatedBy(user.getNome());

        return ResponseEntity.ok(financialRepository.save(fin));
    }

    // --- RELATÓRIOS ---
    @GetMapping("/reports")
    public ResponseEntity<List<ReportDTO>> listReports(@AuthenticationPrincipal User user) {
        UUID tenantId = TenantContext.getCurrentTenant();
        
        if (tenantId == null) {
            tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        }

        if (tenantId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<FinancialReport> reports = reportRepository.findByTenantIdOrderByYearDescCreatedAtDesc(tenantId);
        
        List<ReportDTO> dtos = reports.stream()
                .limit(12)
                .map(r -> new ReportDTO(r.getId().toString(), r.getMonth(), r.getYear(), r.getFileName(), r.getUrl()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/reports/upload")
    @Transactional
    public ResponseEntity<String> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("month") String month,
            @RequestParam("year") int year,
            @AuthenticationPrincipal User user) {

        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            if (user.getTenant() == null) {
                return ResponseEntity.badRequest().body("Usuário sem condomínio associado.");
            }
            tenantId = user.getTenant().getId();
        }

        try {
            String fileUrl = fileStorageService.uploadFile(file);

            FinancialReport report = new FinancialReport();
            report.setTenant(user.getTenant()); // mantém vínculo direto com entity
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