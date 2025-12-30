package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.*;
import com.votzz.backend.repository.CouponRepository;
import com.votzz.backend.repository.PlanoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final CouponRepository couponRepository; 
    private final PlanoRepository planoRepository;   
    private final PasswordEncoder passwordEncoder;

    // ==========================================================
    // 1. GESTÃO DE CUPONS
    // ==========================================================

    @PostMapping("/coupons")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        coupon.setCode(coupon.getCode().toUpperCase());
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<Coupon>> listCoupons() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    // ==========================================================
    // 2. CRIAÇÃO MANUAL DE CONDOMÍNIO
    // ==========================================================
    
    @PostMapping("/create-tenant-manual")
    @Transactional
    public ResponseEntity<String> createTenantManual(@RequestBody ManualTenantDTO dto) {
        if (userRepository.findByEmail(dto.emailSyndic()).isPresent()) {
            return ResponseEntity.badRequest().body("E-mail já cadastrado.");
        }

        var plano = planoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Nenhum plano cadastrado no sistema."));

        Tenant tenant = new Tenant();
        tenant.setNome(dto.condoName());
        tenant.setCnpj(dto.cnpj());
        tenant.setPlano(plano);
        tenant.setAtivo(true);
        tenant.setUnidadesTotal(dto.qtyUnits());
        tenant.setSecretKeyword(dto.secretKeyword());
        tenantRepository.save(tenant);

        User syndic = new User();
        syndic.setNome(dto.nameSyndic());
        syndic.setEmail(dto.emailSyndic());
        syndic.setPassword(passwordEncoder.encode(dto.passwordSyndic()));
        syndic.setRole(Role.SINDICO);
        syndic.setTenant(tenant);
        syndic.setUnidade("ADM");
        syndic.setCpf(dto.cpfSyndic());
        syndic.setWhatsapp("00000000000"); 
        userRepository.save(syndic);

        return ResponseEntity.ok("Condomínio criado com sucesso! O síndico já pode logar.");
    }

    // ==========================================================
    // 3. CRIAR NOVO ADMIN VOTZZ
    // ==========================================================

    @PostMapping("/create-admin")
    public ResponseEntity<String> createNewAdmin(@RequestBody CreateAdminDTO dto) {
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            return ResponseEntity.badRequest().body("E-mail já cadastrado.");
        }

        User admin = new User();
        admin.setNome(dto.nome());
        admin.setEmail(dto.email());
        admin.setPassword(passwordEncoder.encode(dto.password()));
        admin.setRole(Role.ADMIN);
        // Admins não têm Tenant
        
        userRepository.save(admin);
        return ResponseEntity.ok("Novo Administrador Votzz criado com sucesso.");
    }

    // ==========================================================
    // 4. SUPERPODERES DE USUÁRIO (Gestão Total)
    // ==========================================================

    @PatchMapping("/users/{userId}/force-reset-password")
    public ResponseEntity<String> forceResetPassword(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> payload) {
        
        String newPassword = payload.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("A senha deve ter no mínimo 6 caracteres.");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário alvo não encontrado."));

        targetUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(targetUser);

        return ResponseEntity.ok("Senha alterada com sucesso pelo Administrador.");
    }

    @GetMapping("/users")
    public List<UserDTO> listAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDTO(
                    u.getId(), 
                    u.getNome(), 
                    u.getEmail(), 
                    u.getRole().name(), 
                    u.getTenant() != null ? u.getTenant().getNome() : "GLOBAL",
                    u.getLastSeen() // Incluindo última atividade
                ))
                .toList();
    }

    // ==========================================================
    // 5. SUPERPODERES DE CONDOMÍNIO (Gestão Total)
    // ==========================================================

    @PatchMapping("/tenants/{tenantId}/status")
    public ResponseEntity<String> toggleTenantStatus(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Boolean> payload) {
        
        Boolean ativo = payload.get("ativo");
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado."));

        tenant.setAtivo(ativo);
        tenantRepository.save(tenant);

        String status = ativo ? "ATIVADO" : "BLOQUEADO";
        return ResponseEntity.ok("O condomínio " + tenant.getNome() + " foi " + status);
    }

    @PatchMapping("/tenants/{tenantId}/force-secret-keyword")
    public ResponseEntity<String> forceUpdateSecretKeyword(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, String> payload) {

        String newKeyword = payload.get("secretKeyword");
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado."));

        tenant.setSecretKeyword(newKeyword);
        tenantRepository.save(tenant);

        return ResponseEntity.ok("Palavra-chave do condomínio atualizada pelo Admin.");
    }

    // ==========================================================
    // 6. DASHBOARD E MONITORAMENTO (Online Users)
    // ==========================================================

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStats> getStats() {
        long totalCondominios = tenantRepository.count();
        long condominiosAtivos = tenantRepository.countByAtivoTrue();
        long totalUsuarios = userRepository.count();
        
        // [NOVO] Conta usuários ativos nos últimos 5 minutos
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        long onlineUsers = userRepository.countOnlineUsers(fiveMinutesAgo);

        double mrrEstimado = condominiosAtivos * 490.00; 

        return ResponseEntity.ok(new DashboardStats(
            totalCondominios,
            condominiosAtivos,
            totalUsuarios,
            onlineUsers, // Retorna o número de usuários online
            mrrEstimado,
            LocalDateTime.now()
        ));
    }

    // DTOs
    public record UserDTO(UUID id, String nome, String email, String role, String condominio, LocalDateTime lastSeen) {}
    public record DashboardStats(long totalTenants, long activeTenants, long totalUsers, long onlineUsers, double mrr, LocalDateTime generatedAt) {}
    public record ManualTenantDTO(String condoName, String cnpj, Integer qtyUnits, String secretKeyword, String nameSyndic, String emailSyndic, String passwordSyndic, String cpfSyndic) {}
    public record CreateAdminDTO(String nome, String email, String password) {}
}