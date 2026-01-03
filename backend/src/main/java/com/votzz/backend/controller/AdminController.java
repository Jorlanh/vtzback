package com.votzz.backend.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.votzz.backend.domain.Coupon;
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
    public ResponseEntity<AdminDashboardStats> getStats() { return ResponseEntity.ok(adminService.getDashboardStats()); }

    @GetMapping("/organized-users")
    public ResponseEntity<Map<String, Object>> listOrganized() { return ResponseEntity.ok(adminService.listOrganizedUsers()); }

    @GetMapping("/admins")
    public ResponseEntity<List<UserDTO>> listAdmins() { return ResponseEntity.ok(adminService.listAllAdmins()); }

    @GetMapping("/tenants")
    public ResponseEntity<List<Tenant>> listTenants() { return ResponseEntity.ok(adminService.listAllTenants()); }

    @GetMapping("/coupons")
    public ResponseEntity<List<Coupon>> listCoupons() { return ResponseEntity.ok(adminService.listAllCoupons()); }

    // --- POSTs ---
    @PostMapping("/coupons")
    public ResponseEntity<String> createCoupon(@RequestBody CouponDTO dto) {
        adminService.createCoupon(dto.code(), dto.discountPercent(), dto.quantity());
        return ResponseEntity.ok("Cupom criado.");
    }

    @PostMapping("/create-tenant-manual")
    public ResponseEntity<String> createTenant(@RequestBody ManualTenantDTO dto) {
        adminService.createTenantManual(dto);
        return ResponseEntity.ok("Condomínio criado com sucesso.");
    }

    @PostMapping("/create-admin")
    public ResponseEntity<String> createAdmin(@RequestBody Map<String, String> payload) {
        adminService.createNewAdmin(
            payload.get("nome"), payload.get("email"), 
            payload.get("cpf"), payload.get("whatsapp"), payload.get("password")
        );
        return ResponseEntity.ok("Novo Admin criado.");
    }

    @PostMapping("/create-user-linked")
    public ResponseEntity<String> createUserLinked(@RequestBody CreateUserRequest dto) {
        adminService.createUserLinked(dto);
        return ResponseEntity.ok("Usuário adicionado ao condomínio.");
    }

    // --- PUTs / PATCHs ---
    @PutMapping("/tenants/{tenantId}")
    public ResponseEntity<String> updateTenant(@PathVariable UUID tenantId, @RequestBody UpdateTenantDTO dto) {
        adminService.updateTenant(tenantId, dto);
        return ResponseEntity.ok("Condomínio atualizado.");
    }

    @PatchMapping("/tenants/{tenantId}/secret")
    public ResponseEntity<String> updateSecret(@PathVariable UUID tenantId, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok("Palavra-chave alterada.");
    }

    // --- ATUALIZAÇÃO DE USUÁRIO (CORRIGIDO) ---
    @PutMapping("/users/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable UUID userId, @RequestBody UpdateUserRequest req) {
        adminService.adminUpdateUser(userId, req);
        return ResponseEntity.ok("Usuário atualizado.");
    }

    // --- DELETEs ---
    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<String> deleteCoupon(@PathVariable UUID id) {
        adminService.deleteCoupon(id);
        return ResponseEntity.ok("Cupom removido.");
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok("Usuário removido.");
    }

    // --- DTOs Internos (Records) ---
    public record CouponDTO(String code, BigDecimal discountPercent, Integer quantity) {}
    
    public record ManualTenantDTO(
        String condoName, String cnpj, Integer qtyUnits, String secretKeyword, 
        String nameSyndic, String emailSyndic, String passwordSyndic, String cpfSyndic, String phoneSyndic,
        String cep, String logradouro, String numero, String bairro, String cidade, String estado, String pontoReferencia
    ) {}

    public record UpdateTenantDTO(
        String nome, String cnpj, Integer unidadesTotal, Boolean ativo, String secretKeyword,
        String cep, String logradouro, String numero, String bairro, String cidade, String estado, String pontoReferencia
    ) {}

    public record CreateUserRequest(
        String tenantId, String nome, String email, String password, String role, 
        String cpf, String whatsapp, String unidade, String bloco
    ) {}

    // --- DTO CORRIGIDO COM ALIAS PARA EVITAR ERROS DE NOME ---
    public record UpdateUserRequest(
        // Aceita se o front enviar "nome", "nomeCompleto" ou "name"
        @JsonAlias({"name", "nomeCompleto", "fullName"}) 
        String nome, 
        
        String email, 
        
        String cpf, 
        
        // Aceita se o front enviar "whatsapp", "phone" ou "celular"
        @JsonAlias({"phone", "celular", "mobile"}) 
        String whatsapp, 
        
        // Aceita "password", "senha", "newPassword"
        @JsonAlias({"password", "senha"}) 
        String newPassword
    ) {}
}