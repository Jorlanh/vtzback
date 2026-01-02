package com.votzz.backend.controller;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.dto.AdminDashboardStats;
import com.votzz.backend.dto.UserDTO;
import com.votzz.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // --- GETs ---
    @GetMapping("/dashboard-stats")
    public ResponseEntity<AdminDashboardStats> getStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/organized-users")
    public ResponseEntity<Map<String, Object>> listOrganized() {
        return ResponseEntity.ok(adminService.listOrganizedUsers());
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<Tenant>> listTenants() {
        return ResponseEntity.ok(adminService.listAllTenants());
    }

    // --- POSTs ---
    @PostMapping("/coupons")
    public ResponseEntity<String> createCoupon(@RequestBody Map<String, Object> payload) {
        adminService.createCoupon(
            payload.get("code").toString(), 
            new BigDecimal(payload.get("discountPercent").toString()),
            Integer.parseInt(payload.get("quantity").toString())
        );
        return ResponseEntity.ok("Criado.");
    }

    @PostMapping("/create-tenant-manual")
    public ResponseEntity<String> createTenant(@RequestBody ManualTenantDTO dto) {
        adminService.createTenantManual(
            dto.condoName(), dto.cnpj(), dto.qtyUnits(), dto.secretKeyword(),
            dto.nameSyndic(), dto.emailSyndic(), dto.cpfSyndic(), dto.phoneSyndic(), dto.passwordSyndic()
        );
        return ResponseEntity.ok("Sucesso.");
    }

    @PostMapping("/create-admin")
    public ResponseEntity<String> createAdmin(@RequestBody Map<String, String> payload) {
        adminService.createNewAdmin(
            payload.get("nome"), payload.get("email"), 
            payload.get("cpf"), payload.get("whatsapp"), payload.get("password")
        );
        return ResponseEntity.ok("Admin criado.");
    }

    // --- PUTs / PATCHs (SUPORTE) ---
    @PutMapping("/users/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable UUID userId, @RequestBody UpdateUserRequest req) {
        adminService.adminUpdateUser(userId, req.dto(), req.newPassword());
        return ResponseEntity.ok("Dados do usuário atualizados.");
    }

    @PatchMapping("/tenants/{tenantId}/secret")
    public ResponseEntity<String> updateSecret(@PathVariable UUID tenantId, @RequestBody Map<String, String> payload) {
        adminService.updateTenantSecret(tenantId, payload.get("secretKeyword"));
        return ResponseEntity.ok("Palavra-chave do condomínio alterada.");
    }

    // --- DELETE (PROTEGIDO) ---
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok("Usuário removido da plataforma.");
    }

    // Records Auxiliares
    public record ManualTenantDTO(String condoName, String cnpj, Integer qtyUnits, String secretKeyword, String nameSyndic, String emailSyndic, String passwordSyndic, String cpfSyndic, String phoneSyndic) {}
    public record UpdateUserRequest(UserDTO dto, String newPassword) {}
}