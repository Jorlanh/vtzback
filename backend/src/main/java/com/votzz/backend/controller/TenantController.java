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
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Apenas síndicos podem configurar dados bancários.");
        }

        Tenant tenant = user.getTenant();
        
        // Atualiza campos do Tenant
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
        if (user.getTenant() == null) return ResponseEntity.notFound().build();
        
        Tenant t = user.getTenant();
        return ResponseEntity.ok(new BankInfoDTO(
            t.getBanco(),
            t.getAgencia(),
            t.getConta(),
            t.getChavePix(),
            t.getAsaasWalletId()
        ));
    }

    @GetMapping("/my-subscription")
    public ResponseEntity<?> getSubscription(@AuthenticationPrincipal User user) {
        if (user.getTenant() == null) return ResponseEntity.notFound().build();
        Tenant t = user.getTenant();
        return ResponseEntity.ok(Map.of(
            "status", t.getStatusAssinatura() != null ? t.getStatusAssinatura() : "ACTIVE",
            "expirationDate", t.getDataExpiracaoPlano() != null ? t.getDataExpiracaoPlano().toString() : LocalDate.now().plusDays(30).toString(),
            "plan", t.getPlano() != null ? t.getPlano().getNome() : "Básico"
        ));
    }

    // --- ENDPOINT DE AUDITORIA ---
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs(@AuthenticationPrincipal User user) {
        // 1. Se for Morador/Síndico/AdmCondo (TEM TENANT)
        if (user.getTenant() != null) {
            return ResponseEntity.ok(auditLogRepository.findByTenantIdOrderByCreatedAtDesc(user.getTenant().getId()));
        }
        
        // 2. Se for Admin Votzz (SEM TENANT) - Role.ADMIN
        if (user.getRole() == Role.ADMIN) {
            // Retorna TODOS os logs do sistema
            return ResponseEntity.ok(auditLogRepository.findAllByOrderByCreatedAtDesc());
        }

        // 3. Fallback: Se não tem tenant e não é admin, retorna lista vazia em vez de erro
        return ResponseEntity.ok(List.of());
    }

    @PatchMapping("/secret-keyword")
    public ResponseEntity<String> updateSecretKeyword(@RequestBody Map<String, String> payload, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADMIN) return ResponseEntity.status(403).build();

        Tenant tenant = user.getTenant();
        String newKeyword = payload.get("secretKeyword");
        if (newKeyword == null || newKeyword.length() < 4) return ResponseEntity.badRequest().body("Mínimo 4 caracteres.");

        tenant.setSecretKeyword(newKeyword);
        tenantRepository.save(tenant);
        return ResponseEntity.ok("Palavra-chave atualizada.");
    }

    public record TenantDTO(UUID id, String nome) {}
    public record BankInfoDTO(String bankName, String agency, String account, String pixKey, String asaasWalletId) {}
}