package com.votzz.backend.controller;

import com.votzz.backend.domain.TenantPaymentConfig;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.TenantPaymentConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants/payment-config")
public class TenantPaymentConfigController {

    @Autowired
    private TenantPaymentConfigRepository configRepository;

    @GetMapping
    public ResponseEntity<TenantPaymentConfig> getConfig(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getTenant() == null) return ResponseEntity.badRequest().build();
        
        return configRepository.findByTenantId(currentUser.getTenant().getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    // RETORNO PADRÃO SEGURO:
                    // Retorna um objeto JSON com configurações padrão sem salvar no banco ainda.
                    // Isso evita o erro 500.
                    TenantPaymentConfig defaultConfig = new TenantPaymentConfig();
                    defaultConfig.setEnableManualPix(true); // Padrão: Pix Manual ativo
                    defaultConfig.setEnableAsaas(false);
                    // Não definimos o 'tenant' aqui para evitar recursão infinita no JSON se não configurado
                    return ResponseEntity.ok(defaultConfig);
                });
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> saveConfig(@RequestBody TenantPaymentConfig dto, @AuthenticationPrincipal User currentUser) {
        if (currentUser.getTenant() == null) return ResponseEntity.badRequest().build();
        
        // Apenas Síndico, Admin ou Gerente pode alterar
        if (currentUser.getRole() != Role.SINDICO && currentUser.getRole() != Role.ADM_CONDO && currentUser.getRole() != Role.MANAGER) {
            return ResponseEntity.status(403).body("Acesso negado.");
        }

        TenantPaymentConfig config = configRepository.findByTenantId(currentUser.getTenant().getId())
                .orElse(new TenantPaymentConfig());

        // Vincula ao condomínio do usuário logado
        config.setTenant(currentUser.getTenant());
        
        // Atualiza campos
        config.setEnableAsaas(dto.isEnableAsaas());
        config.setEnableManualPix(dto.isEnableManualPix());
        config.setBankName(dto.getBankName());
        config.setAgency(dto.getAgency());
        config.setAccount(dto.getAccount());
        config.setPixKey(dto.getPixKey());
        config.setInstructions(dto.getInstructions());
        
        // Se tiver campos do Asaas (API Key), salvar também
        // config.setAsaasApiKey(dto.getAsaasApiKey());

        configRepository.save(config);
        return ResponseEntity.ok("Configurações de pagamento salvas com sucesso.");
    }
}