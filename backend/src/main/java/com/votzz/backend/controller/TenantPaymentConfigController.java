package com.votzz.backend.controller;

import com.votzz.backend.domain.TenantPaymentConfig;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.TenantPaymentConfigRepository;
import com.votzz.backend.service.AuditService; // Import da Auditoria
import lombok.RequiredArgsConstructor; // Lombok para injeção limpa
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants/payment-config")
@RequiredArgsConstructor // Injeta repositório e auditService automaticamente
public class TenantPaymentConfigController {

    private final TenantPaymentConfigRepository configRepository;
    private final AuditService auditService; // Serviço de Auditoria

    @GetMapping
    public ResponseEntity<TenantPaymentConfig> getConfig(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getTenant() == null) return ResponseEntity.badRequest().build();
        
        return configRepository.findByTenantId(currentUser.getTenant().getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    // Retorna padrão se não existir
                    TenantPaymentConfig defaultConfig = new TenantPaymentConfig();
                    defaultConfig.setEnableManualPix(true);
                    defaultConfig.setEnableAsaas(false);
                    return ResponseEntity.ok(defaultConfig);
                });
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> saveConfig(@RequestBody TenantPaymentConfig dto, @AuthenticationPrincipal User currentUser) {
        if (currentUser.getTenant() == null) return ResponseEntity.badRequest().build();
        
        // Segurança: Apenas Síndico ou Admin
        if (currentUser.getRole() != Role.SINDICO && currentUser.getRole() != Role.ADM_CONDO && currentUser.getRole() != Role.MANAGER) {
            return ResponseEntity.status(403).body("Acesso negado. Apenas síndicos podem alterar dados financeiros.");
        }

        TenantPaymentConfig config = configRepository.findByTenantId(currentUser.getTenant().getId())
                .orElse(new TenantPaymentConfig());

        config.setTenant(currentUser.getTenant());
        
        // Atualiza campos
        config.setEnableAsaas(dto.isEnableAsaas());
        config.setEnableManualPix(dto.isEnableManualPix());
        config.setBankName(dto.getBankName());
        config.setAgency(dto.getAgency());
        config.setAccount(dto.getAccount());
        config.setPixKey(dto.getPixKey());
        config.setInstructions(dto.getInstructions());
        
        // Salva a chave do Asaas se fornecida
        if (dto.getAsaasAccessToken() != null && !dto.getAsaasAccessToken().isEmpty()) {
            config.setAsaasAccessToken(dto.getAsaasAccessToken());
        }

        configRepository.save(config);

        // --- AUDITORIA DE SEGURANÇA ---
        auditService.log(
            currentUser, 
            currentUser.getTenant(), 
            "ATUALIZAR_FINANCEIRO", 
            "Alterou configurações de recebimento (Asaas/Pix Manual).", 
            "FINANCEIRO"
        );

        return ResponseEntity.ok("Configurações de pagamento salvas com sucesso.");
    }
}