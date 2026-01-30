package com.votzz.backend.controller;

import com.votzz.backend.domain.User;
import com.votzz.backend.dto.AdminDashboardStats;
import com.votzz.backend.service.CondoDashboardService;
import com.votzz.backend.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/condo/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CondoDashboardController {

    private final CondoDashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStats> getStats(@AuthenticationPrincipal User currentUser) {
        // Tenta pegar o Tenant do contexto (Interceptor)
        UUID tenantId = TenantContext.getCurrentTenant();
        
        // Fallback: Se não vier no header, pega do usuário logado (IMPORTANTE PARA MORADORES)
        if (tenantId == null && currentUser != null && currentUser.getTenant() != null) {
            tenantId = currentUser.getTenant().getId();
        }
        
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // O Service agora retorna TUDO (Saldo + Usuários), sem distinção de cargo
        return ResponseEntity.ok(dashboardService.getCondoStats(tenantId));
    }
}