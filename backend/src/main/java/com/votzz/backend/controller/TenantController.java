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
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADMIN) {
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

    // --- ENDPOINT DE AUDITORIA (CORRIGIDO E BLINDADO) ---
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@AuthenticationPrincipal User user) {
        List<AuditLog> logs;

        // 1. Lógica de decisão: Quem pode ver o quê?
        if (user.getTenant() != null) {
            // Se tem condomínio, vê apenas os dele
            logs = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(user.getTenant().getId());
        } else if (user.getRole() == Role.ADMIN) {
            // Se é Admin Geral (sem condomínio), vê tudo
            logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        } else {
            // Se não é nenhum dos dois, retorna lista vazia (evita erro 400/403)
            return ResponseEntity.ok(List.of());
        }

        // 2. CONVERSÃO MANUAL (DTO):
        // Isto extrai apenas os dados seguros e ignora o objeto "Tenant" problemático.
        List<AuditLogResponse> response = logs.stream()
            .map(log -> new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getUserName(),  // Nome do utilizador
                log.getUserId(),    // ID do utilizador
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
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADMIN) return ResponseEntity.status(403).build();

        Tenant tenant = user.getTenant();
        if (tenant == null) return ResponseEntity.badRequest().body("Sem condomínio.");

        String newKeyword = payload.get("secretKeyword");
        if (newKeyword == null || newKeyword.length() < 4) return ResponseEntity.badRequest().body("Mínimo 4 caracteres.");

        tenant.setSecretKeyword(newKeyword);
        tenantRepository.save(tenant);
        return ResponseEntity.ok("Palavra-chave atualizada.");
    }

    // --- DTOs (Objetos de Transferência de Dados) ---
    public record TenantDTO(UUID id, String nome) {}
    public record BankInfoDTO(String bankName, String agency, String account, String pixKey, String asaasWalletId) {}
    
    // Este DTO é a chave para resolver o problema de serialização
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