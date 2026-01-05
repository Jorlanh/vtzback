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
        UUID tenantId = TenantContext.getTenant();
        if (tenantId == null && currentUser != null && currentUser.getTenant() != null) {
            tenantId = currentUser.getTenant().getId();
        }
        
        if (tenantId == null) return ResponseEntity.badRequest().build();
        
        return ResponseEntity.ok(dashboardService.getCondoStats(tenantId));
    }
}