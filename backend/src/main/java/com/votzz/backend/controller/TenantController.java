package com.votzz.backend.controller;

import com.votzz.backend.core.tenant.TenantContext;
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
    public ResponseEntity<String> saveBankInfo(@RequestBody BankInfoDTO dto, @AuthenticationPrincipal User user) {
        UUID tenantId = resolveTenantId(user);
        if (tenantId == null) {
            return ResponseEntity.badRequest().body("Usuário sem condomínio vinculado.");
        }

        if (!hasManagerRole(user)) {
            return ResponseEntity.status(403).body("Apenas síndicos ou admins podem configurar dados bancários.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));

        tenant.setBanco(dto.bankName());
        tenant.setAgencia(dto.agency());
        tenant.setConta(dto.account());
        tenant.setChavePix(dto.pixKey());

        if (dto.asaasWalletId() != null && !dto.asaasWalletId().isBlank()) {
            tenant.setAsaasWalletId(dto.asaasWalletId());
        }

        tenantRepository.save(tenant);
        return ResponseEntity.ok("Dados bancários atualizados com sucesso.");
    }

    // --- OBTER DADOS BANCÁRIOS ---
    @GetMapping("/bank-info")
    public ResponseEntity<BankInfoDTO> getBankInfo(@AuthenticationPrincipal User user) {
        UUID tenantId = resolveTenantId(user);
        if (tenantId == null) {
            return ResponseEntity.ok(new BankInfoDTO("", "", "", "", ""));
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return ResponseEntity.ok(new BankInfoDTO("", "", "", "", ""));
        }

        return ResponseEntity.ok(new BankInfoDTO(
                tenant.getBanco() != null ? tenant.getBanco() : "",
                tenant.getAgencia() != null ? tenant.getAgencia() : "",
                tenant.getConta() != null ? tenant.getConta() : "",
                tenant.getChavePix() != null ? tenant.getChavePix() : "",
                tenant.getAsaasWalletId() != null ? tenant.getAsaasWalletId() : ""
        ));
    }

    // --- CORREÇÃO DEFINITIVA DO ERRO 400 NO DASHBOARD ---
    @GetMapping("/my-subscription")
    public ResponseEntity<MySubscriptionResponse> getMySubscription(@AuthenticationPrincipal User user) {
        UUID tenantId = resolveTenantId(user);

        if (tenantId == null) {
            return ResponseEntity.ok(new MySubscriptionResponse(
                    false,
                    "NO_TENANT",
                    "Nenhum condomínio associado",
                    null
            ));
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));

        MySubscriptionResponse response = new MySubscriptionResponse(
                tenant.isSubscriptionActive(),
                tenant.getStatusAssinatura() != null ? tenant.getStatusAssinatura() : "PENDING",
                tenant.getPlano() != null ? tenant.getPlano().getNome() : "Básico",
                tenant.getDataExpiracaoPlano() != null ? tenant.getDataExpiracaoPlano().toString() : null
        );

        return ResponseEntity.ok(response);
    }

    // --- DELETE (SOFT) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        if (user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }

        tenant.setAtivo(false);
        tenantRepository.save(tenant);

        // Log de auditoria
        AuditLog log = new AuditLog();
        log.setAction("EXCLUIR_CONDOMINIO");
        log.setDetails("Soft delete do condomínio: " + tenant.getNome());
        log.setUserId(user.getId().toString());
        log.setUserName(user.getNome());
        log.setResourceType("TENANT");
        log.setTimestamp(LocalDateTime.now().toString());
        auditLogRepository.save(log);

        return ResponseEntity.ok().build();
    }

    // --- AUDIT LOGS ---
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@AuthenticationPrincipal User user) {
        UUID tenantId = resolveTenantId(user);
        List<AuditLog> logs;

        if (tenantId != null && hasManagerRole(user)) {
            logs = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        } else if (user.getRole() == Role.ADMIN) {
            logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        } else {
            logs = List.of();
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

    // --- UPDATE SECRET KEYWORD ---
    @PatchMapping("/secret-keyword")
    public ResponseEntity<String> updateSecretKeyword(@RequestBody Map<String, String> payload, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!hasManagerRole(user)) {
            return ResponseEntity.status(403).body("Acesso negado.");
        }

        UUID tenantId = resolveTenantId(user);
        if (tenantId == null) {
            return ResponseEntity.badRequest().body("Sem condomínio associado.");
        }

        String newKeyword = payload.get("secretKeyword");
        if (newKeyword == null || newKeyword.length() < 4) {
            return ResponseEntity.badRequest().body("A palavra-chave deve ter no mínimo 4 caracteres.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));

        tenant.setSecretKeyword(newKeyword);
        tenantRepository.save(tenant);

        return ResponseEntity.ok("Palavra-chave atualizada com sucesso.");
    }

    // Métodos auxiliares reutilizáveis
    private UUID resolveTenantId(User user) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return tenantId;
        }
        return user.getTenant() != null ? user.getTenant().getId() : null;
    }

    private boolean hasManagerRole(User user) {
        return user.getRole() == Role.SINDICO ||
               user.getRole() == Role.ADMIN ||
               user.getRole() == Role.ADM_CONDO;
    }

    // Records (mantidos iguais, mas com nomes mais claros)
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

    // Resposta padronizada para subscription (evita quebras no frontend)
    public record MySubscriptionResponse(
            boolean active,
            String status,
            String plan,
            String expirationDate  // ISO string ou null
    ) {}
}