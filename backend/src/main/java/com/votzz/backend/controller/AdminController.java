package com.votzz.backend.controller;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.TenantRepository;
import com.votzz.backend.repository.UserRepository;
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
    private final PasswordEncoder passwordEncoder;

    // ==========================================================
    // 1. SUPERPODERES DE USUÁRIO
    // ==========================================================

    // Admin da Votzz troca a senha de QUALQUER pessoa (Morador, Síndico, Afiliado)
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

    // Listar todos os usuários do sistema (Raio-X)
    @GetMapping("/users")
    public List<UserDTO> listAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDTO(
                    u.getId(), 
                    u.getNome(), 
                    u.getEmail(), 
                    u.getRole().name(), 
                    u.getTenant() != null ? u.getTenant().getNome() : "GLOBAL"
                ))
                .toList();
    }

    // ==========================================================
    // 2. SUPERPODERES DE CONDOMÍNIO (TENANT)
    // ==========================================================

    // Bloquear ou Ativar um Condomínio (Ex: Inadimplência)
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

    // Recuperar/Alterar a Palavra-Chave Secreta de um Condomínio
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
    // 3. ANÁLISE E DASHBOARD (Analytics)
    // ==========================================================

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStats> getStats() {
        long totalCondominios = tenantRepository.count();
        long condominiosAtivos = tenantRepository.countByAtivoTrue();
        long totalUsuarios = userRepository.count();
        
        // Simulação de receita (MRR) - Em produção viria da tabela de pagamentos
        // Aqui pegamos quantos Tenants Ativos existem e multiplicamos por um ticket médio estimado
        double mrrEstimado = condominiosAtivos * 490.00; 

        return ResponseEntity.ok(new DashboardStats(
            totalCondominios,
            condominiosAtivos,
            totalUsuarios,
            mrrEstimado,
            LocalDateTime.now()
        ));
    }

    // DTOs Auxiliares
    public record UserDTO(UUID id, String nome, String email, String role, String condominio) {}
    public record DashboardStats(long totalTenants, long activeTenants, long totalUsers, double mrr, LocalDateTime generatedAt) {}
}