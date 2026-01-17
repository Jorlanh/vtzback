package com.votzz.backend.controller;

import com.votzz.backend.domain.AuditLog;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.AuditLogRepository;
import com.votzz.backend.repository.TenantRepository;
import com.votzz.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TenantController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/public-list")
    public List<TenantDTO> listPublic() {
        return tenantRepository.findAll().stream()
                .filter(Tenant::isAtivo)
                .map(t -> new TenantDTO(t.getId(), t.getNome()))
                .toList();
    }

    // --- SALVAR DADOS BANCÁRIOS ---
    @PostMapping("/bank-info")
    public ResponseEntity<?> saveBankInfo(@RequestBody BankInfoDTO dto, @AuthenticationPrincipal User user) {
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADMIN && user.getRole() != Role.ADM_CONDO) {
            return ResponseEntity.status(403).body("Apenas síndicos podem configurar dados bancários.");
        }

        Tenant tenant = user.getTenant();
        if (tenant == null) return ResponseEntity.badRequest().body("Usuário sem condomínio vinculado.");
        
        tenant.setBanco(dto.bankName());
        tenant.setAgencia(dto.agency());
        tenant.setConta(dto.account());
        tenant.setChavePix(dto.pixKey());
        
        if(dto.asaasWalletId() != null && !dto.asaasWalletId().isBlank()){
            tenant.setAsaasWalletId(dto.asaasWalletId());
        }
        
        tenantRepository.save(tenant);
        return ResponseEntity.ok("Dados bancários atualizados com sucesso.");
    }

    // --- OBTER DADOS BANCÁRIOS ---
    @GetMapping("/bank-info")
    public ResponseEntity<BankInfoDTO> getBankInfo(@AuthenticationPrincipal User user) {
        if (user.getTenant() == null) {
            // Retorna vazio em vez de erro para não quebrar o frontend
            return ResponseEntity.ok(new BankInfoDTO("", "", "", "", ""));
        }
        
        Tenant t = user.getTenant();
        return ResponseEntity.ok(new BankInfoDTO(
            t.getBanco() != null ? t.getBanco() : "",
            t.getAgencia() != null ? t.getAgencia() : "",
            t.getConta() != null ? t.getConta() : "",
            t.getChavePix() != null ? t.getChavePix() : "",
            t.getAsaasWalletId() != null ? t.getAsaasWalletId() : ""
        ));
    }

    // --- CORREÇÃO DO ERRO 400 NO DASHBOARD ---
    @GetMapping("/my-subscription")
    public ResponseEntity<?> getSubscription(@AuthenticationPrincipal User user) {
        if (user.getTenant() == null) {
            // Retorna um objeto padrão "inativo" ou vazio, mas com status 200 OK
            return ResponseEntity.ok(Map.of(
                "status", "NO_TENANT",
                "expirationDate", "",
                "plan", "Nenhum"
            ));
        }
        Tenant t = user.getTenant();
        return ResponseEntity.ok(Map.of(
            "status", t.getStatusAssinatura() != null ? t.getStatusAssinatura() : "ACTIVE",
            "expirationDate", t.getDataExpiracaoPlano() != null ? t.getDataExpiracaoPlano().toString() : LocalDate.now().plusDays(30).toString(),
            "plan", t.getPlano() != null ? t.getPlano().getNome() : "Básico"
        ));
    }

    // --- NOVO: DELETE (SOFT DELETE) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTenant(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        if (user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Acesso negado.");
        }
        
        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) return ResponseEntity.notFound().build();
        
        tenant.setAtivo(false); // SOFT DELETE
        tenantRepository.save(tenant);
        
        AuditLog log = new AuditLog();
        log.setAction("EXCLUIR_CONDOMINIO");
        log.setDetails("Soft delete via API em " + tenant.getNome());
        log.setUserId(user.getId().toString());
        log.setUserName(user.getNome());
        log.setResourceType("ADMIN_PANEL");
        log.setTimestamp(LocalDateTime.now().toString());
        auditLogRepository.save(log);

        return ResponseEntity.ok().build();
    }

    // --- ENDPOINT DE AUDITORIA ---
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@AuthenticationPrincipal User user) {
        List<AuditLog> logs;

        if (user.getTenant() != null) {
            logs = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(user.getTenant().getId());
        } else if (user.getRole() == Role.ADMIN) {
            logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        } else {
            return ResponseEntity.ok(List.of());
        }

        List<AuditLogResponse> response = logs.stream()
            .map(log -> new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getUserName(),
                log.getUserId(), 
                log.getDetails(),
                log.getTimestamp(),
                log.getResourceType(),
                log.getIpAddress()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/secret-keyword")
    public ResponseEntity<String> updateSecretKeyword(@RequestBody Map<String, String> payload, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADMIN && user.getRole() != Role.ADM_CONDO) return ResponseEntity.status(403).build();

        Tenant tenant = user.getTenant();
        if (tenant == null) return ResponseEntity.badRequest().body("Sem condomínio.");

        String newKeyword = payload.get("secretKeyword");
        if (newKeyword == null || newKeyword.length() < 4) return ResponseEntity.badRequest().body("Mínimo 4 caracteres.");

        tenant.setSecretKeyword(newKeyword);
        tenantRepository.save(tenant);
        return ResponseEntity.ok("Palavra-chave atualizada.");
    }

    public record TenantDTO(UUID id, String nome) {}
    public record BankInfoDTO(String bankName, String agency, String account, String pixKey, String asaasWalletId) {}
    
    public record AuditLogResponse(
        UUID id, 
        String action, 
        String userName, 
        String userId, 
        String details, 
        String timestamp, 
        String resourceType, 
        String ipAddress
    ) {}
}