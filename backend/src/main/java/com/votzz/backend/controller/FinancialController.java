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
// @CrossOrigin removido para evitar conflito com SecurityConfig
public class FinancialController {

    private final CondoFinancialRepository financialRepository;
    private final FinancialReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    // --- SALDO ---
    @GetMapping("/balance")
    public ResponseEntity<CondoFinancial> getBalance(@AuthenticationPrincipal User user) {
        System.out.println(">>> GET /api/financial/balance - Iniciando...");
        
        UUID tenantId = resolveTenantId(user);
        
        if (tenantId == null) {
            System.out.println("ERRO: Tenant ID nulo. Retornando objeto vazio.");
            return ResponseEntity.ok(new CondoFinancial());
        }

        System.out.println("Buscando financeiro para Tenant: " + tenantId);
        return financialRepository.findByTenantId(tenantId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    System.out.println("Nenhum registro encontrado. Retornando novo.");
                    return ResponseEntity.ok(new CondoFinancial());
                });
    }

    @PostMapping("/update")
    @Transactional
    public ResponseEntity<CondoFinancial> updateBalance(
            @RequestBody Map<String, BigDecimal> payload,
            @AuthenticationPrincipal User user) {

        System.out.println(">>> POST /api/financial/update - Iniciando...");
        UUID tenantId = resolveTenantId(user);

        if (tenantId == null) {
            System.out.println("ERRO: Tenant não identificado. Acesso negado.");
            return ResponseEntity.status(403).build();
        }

        CondoFinancial fin = financialRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    CondoFinancial newFin = new CondoFinancial();
                    newFin.setTenant(user.getTenant()); 
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
        System.out.println(">>> GET /api/financial/reports - Iniciando...");
        UUID tenantId = resolveTenantId(user);

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

        UUID tenantId = resolveTenantId(user);
        if (tenantId == null) {
            return ResponseEntity.badRequest().body("Usuário sem condomínio associado.");
        }

        try {
            String fileUrl = fileStorageService.uploadFile(file);

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

    // Método auxiliar para resolver o ID do Tenant com segurança
    private UUID resolveTenantId(User user) {
        // 1. Tenta pegar do contexto (Header X-Tenant-ID processado pelo filtro)
        UUID contextId = TenantContext.getCurrentTenant();
        if (contextId != null) return contextId;

        // 2. Fallback: Tenta pegar do usuário logado (Campo único)
        if (user.getTenant() != null) return user.getTenant().getId();

        // 3. Fallback: Tenta pegar da lista de tenants
        if (user.getTenants() != null && !user.getTenants().isEmpty()) {
            return user.getTenants().get(0).getId();
        }

        return null;
    }

    public record ReportDTO(String id, String month, int year, String fileName, String url) {}
}