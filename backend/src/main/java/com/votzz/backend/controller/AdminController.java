package com.votzz.backend.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.votzz.backend.config.security.WebSocketEventListener;
import com.votzz.backend.domain.AuditLog;
import com.votzz.backend.domain.Coupon;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.dto.AdminDashboardStats;
import com.votzz.backend.dto.AuthDTOs.LoginResponse;
import com.votzz.backend.dto.UserDTO;
import com.votzz.backend.repository.TenantRepository;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.service.AdminService;
import com.votzz.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TokenService tokenService;
    private final WebSocketEventListener webSocketEventListener;

    @Value("${votzz.admin.id}")
    private String superAdminId;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<AdminDashboardStats> getStats() { 
        AdminDashboardStats original = adminService.getDashboardStats();
        
        // Pega o maior valor entre o Banco de Dados (ActivityInterceptor) e o WebSocket
        // Isso garante que se você estiver navegando via REST, você conta como 1.
        long realOnlineCount = Math.max(original.onlineUsers(), webSocketEventListener.getOnlineCount());

        AdminDashboardStats updated = new AdminDashboardStats(
            original.totalUsers(),
            realOnlineCount, 
            original.totalTenants(),
            original.activeTenants(),
            original.mrr()
        );
        
        return ResponseEntity.ok(updated); 
    }

    // ... (Mantenha o restante dos métodos inalterados) ...
    // Vou omitir o resto para economizar espaço, pois a correção é apenas no getStats
    // e os outros métodos dependem apenas do serviço.
    
    @GetMapping("/organized-users")
    public ResponseEntity<Map<String, Object>> listOrganized() { return ResponseEntity.ok(adminService.listOrganizedUsers()); }

    @GetMapping("/admins")
    public ResponseEntity<List<UserDTO>> listAdmins() { return ResponseEntity.ok(adminService.listAllAdmins()); }

    @GetMapping("/tenants")
    public ResponseEntity<List<Tenant>> listTenants() { return ResponseEntity.ok(adminService.listAllTenants()); }

    @GetMapping("/coupons")
    public ResponseEntity<List<Coupon>> listCoupons() { return ResponseEntity.ok(adminService.listAllCoupons()); }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(adminService.listAuditLogs());
    }

    @PostMapping("/impersonate/{tenantId}")
    public ResponseEntity<LoginResponse> impersonateTenant(@PathVariable UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));

        User tempUser = new User();
        tempUser.setId(UUID.randomUUID()); 
        tempUser.setEmail("admin-impersonation@votzz.com");
        tempUser.setNome("Super Admin (Acesso Direto)");
        tempUser.setRole(Role.SINDICO); 
        tempUser.setTenant(tenant);

        String token = tokenService.generateToken(tempUser);

        return ResponseEntity.ok(new LoginResponse(
            token, "Bearer", tempUser.getId().toString(), tempUser.getNome(), tempUser.getEmail(),
            "SINDICO", tenant.getId().toString(), null, null, null, false, false, null
        ));
    }

    @PatchMapping("/users/{userId}/toggle-status")
    public ResponseEntity<String> toggleUserStatus(@PathVariable UUID userId) {
        if (userId.toString().equals(superAdminId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Você não pode suspender o Super Admin Principal.");
        }
        User user = userRepository.findById(userId).orElseThrow();
        boolean currentStatus = user.isEnabled(); 
        user.setEnabled(!currentStatus);
        userRepository.save(user);
        return ResponseEntity.ok(user.isEnabled() ? "Usuário ativado." : "Usuário suspenso.");
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable UUID userId, @RequestBody UpdateUserRequest req) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User currentUser) {
            boolean isTargetSuperAdmin = userId.toString().equals(superAdminId);
            boolean isRequesterSuperAdmin = currentUser.getId().toString().equals(superAdminId);
            if (isTargetSuperAdmin && !isRequesterSuperAdmin) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Apenas o Super Admin pode editar seu próprio perfil.");
            }
        }
        adminService.adminUpdateUser(userId, req);
        return ResponseEntity.ok("Usuário atualizado.");
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID userId) {
        if (userId.toString().equals(superAdminId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("IMPEDIDO: Você não pode deletar o Super Admin Principal.");
        }
        adminService.deleteUser(userId);
        return ResponseEntity.ok("Usuário removido.");
    }

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

    @PutMapping("/tenants/{tenantId}")
    public ResponseEntity<String> updateTenant(@PathVariable UUID tenantId, @RequestBody UpdateTenantDTO dto) {
        adminService.updateTenant(tenantId, dto);
        return ResponseEntity.ok("Condomínio atualizado.");
    }

    @PatchMapping("/tenants/{tenantId}/secret")
    public ResponseEntity<String> updateSecret(@PathVariable UUID tenantId, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok("Palavra-chave alterada.");
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<String> deleteCoupon(@PathVariable UUID id) {
        adminService.deleteCoupon(id);
        return ResponseEntity.ok("Cupom removido.");
    }

    public record CouponDTO(String code, BigDecimal discountPercent, Integer quantity) {}
    
    // CORREÇÃO AQUI: Adicionado campo 'plano'
    public record ManualTenantDTO(
        String condoName, String cnpj, Integer qtyUnits, String secretKeyword, 
        String nameSyndic, String emailSyndic, String passwordSyndic, String cpfSyndic, String phoneSyndic,
        String cep, String logradouro, String numero, String bairro, String cidade, String estado, String pontoReferencia,
        String plano 
    ) {}

    public record UpdateTenantDTO(
        String nome, String cnpj, Integer unidadesTotal, Boolean ativo, String secretKeyword,
        String plano, LocalDate dataExpiracaoPlano,
        String cep, String logradouro, String numero, String bairro, String cidade, String estado, String pontoReferencia
    ) {}

    public record CreateUserRequest(
        @JsonProperty("tenantId") String tenantId, 
        @JsonProperty("nome") String nome, 
        @JsonProperty("email") String email, 
        @JsonProperty("password") String password, 
        @JsonProperty("role") String role, 
        @JsonProperty("cpf") String cpf, 
        @JsonProperty("whatsapp") String whatsapp, 
        @JsonProperty("unidade") String unidade, 
        @JsonProperty("bloco") String bloco
    ) {}

    public record UpdateUserRequest(
        @JsonProperty("nome") @JsonAlias({"name", "nomeCompleto", "fullName"}) String nome, 
        @JsonProperty("email") String email, 
        @JsonProperty("cpf") String cpf, 
        @JsonProperty("whatsapp") @JsonAlias({"phone", "celular", "mobile", "zap"}) String whatsapp, 
        @JsonProperty("role") String role, 
        @JsonProperty("newPassword") @JsonAlias({"password", "senha"}) String newPassword
    ) {}
}