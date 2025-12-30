package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role; // Garanta que Role esteja importado corretamente
import com.votzz.backend.repository.*;
import com.votzz.backend.service.EmailService;
import com.votzz.backend.service.TokenService;
import com.votzz.backend.dto.*; 
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.security.Principal;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PlanoRepository planoRepository;
    private final CouponRepository couponRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;

    // --- 1. LOGIN (HÍBRIDO: EMAIL OU CPF) ---
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // Busca por Email OU CPF
        User user = userRepository.findByEmailOrCpf(request.login(), request.login())
                .orElseThrow(() -> new RuntimeException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Usuário ou senha inválidos");
        }

        // Atualiza status "Online"
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        String token = tokenService.generateToken(user);

        // [CORREÇÃO CRÍTICA] Agora enviamos Bloco e Unidade na resposta
        return ResponseEntity.ok(new LoginResponse(
            token, 
            "Bearer", 
            user.getId().toString(), 
            user.getNome(), 
            user.getEmail(),
            user.getRole().name(), 
            user.getTenant() != null ? user.getTenant().getId().toString() : null,
            user.getBloco(),   // [NOVO] Enviando Bloco
            user.getUnidade(), // [NOVO] Enviando Unidade
            user.getCpf()      // [NOVO] Enviando CPF
        ));
    }

    // --- 2. VALIDAÇÃO DE CUPOM ---
    @GetMapping("/validate-coupon/{code}")
    public ResponseEntity<?> validateCoupon(@PathVariable String code) {
        var couponOpt = couponRepository.findByCode(code.toUpperCase());
        if (couponOpt.isEmpty() || !couponOpt.get().isActive()) {
            return ResponseEntity.badRequest().body("Cupom inválido ou expirado.");
        }
        return ResponseEntity.ok(Map.of("code", couponOpt.get().getCode(), "discountPercent", couponOpt.get().getDiscountPercent()));
    }

    // --- 3. CADASTRO DE SÍNDICO ---
    @PostMapping("/register-condo")
    @Transactional
    public ResponseEntity<String> registerCondo(@RequestBody RegisterCondoRequest request) {
        if (userRepository.findByEmail(request.emailSyndic()).isPresent()) {
            return ResponseEntity.badRequest().body("Este e-mail já está cadastrado.");
        }
        
        if (request.cpfSyndic() != null && userRepository.existsByCpf(request.cpfSyndic())) {
            return ResponseEntity.badRequest().body("Este CPF já está cadastrado.");
        }
        
        Plano plano = planoRepository.findById(java.util.UUID.fromString(request.planId()))
                .orElseThrow(() -> new RuntimeException("Plano não encontrado"));

        Tenant tenant = new Tenant();
        tenant.setNome(request.condoName());
        tenant.setCnpj(request.cnpj());
        tenant.setPlano(plano);
        tenant.setAtivo(true);
        tenant.setUnidadesTotal(request.qtyUnits());
        tenant.setSecretKeyword(request.secretKeyword());
        tenantRepository.save(tenant);

        User syndic = new User();
        syndic.setNome(request.nameSyndic());
        syndic.setEmail(request.emailSyndic());
        syndic.setPassword(passwordEncoder.encode(request.passwordSyndic()));
        syndic.setRole(Role.SINDICO);
        syndic.setTenant(tenant);
        syndic.setUnidade("ADM");
        syndic.setCpf(request.cpfSyndic());
        syndic.setWhatsapp(request.whatsappSyndic());
        userRepository.save(syndic);

        return ResponseEntity.ok("Condomínio e Síndico cadastrados com sucesso!");
    }

    // --- 4. CADASTRO DE MORADOR ---
    @PostMapping("/register-resident")
    @Transactional
    public ResponseEntity<String> registerResident(@RequestBody ResidentRegisterRequest request) {
        Tenant tenant = tenantRepository.findByCnpjOrNome(request.condoIdentifier())
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado. Verifique o Nome ou CNPJ digitado."));

        if (!tenant.getSecretKeyword().equals(request.secretKeyword())) {
            return ResponseEntity.badRequest().body("Palavra-chave do condomínio incorreta.");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body("E-mail já cadastrado.");
        }

        if (request.cpf() != null && userRepository.existsByCpf(request.cpf())) {
            return ResponseEntity.badRequest().body("CPF já cadastrado.");
        }

        User user = new User();
        user.setNome(request.nome());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCpf(request.cpf());
        user.setWhatsapp(request.whatsapp()); 
        user.setUnidade(request.unidade());
        user.setBloco(request.bloco()); 
        user.setRole(Role.MORADOR);
        user.setTenant(tenant);

        userRepository.save(user);

        return ResponseEntity.ok("Cadastro realizado com sucesso! Faça login.");
    }

    // --- OUTROS ---
    @PatchMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) return ResponseEntity.badRequest().body("Senha incorreta.");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Senha alterada!");
    }

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("E-mail não encontrado."));
        resetTokenRepository.findByUser(user).ifPresent(resetTokenRepository::delete);
        String code = String.format("%06d", new Random().nextInt(999999));
        resetTokenRepository.save(new PasswordResetToken(user, code));
        emailService.sendResetToken(user.getEmail(), code);
        return ResponseEntity.ok("Código enviado para: " + user.getEmail());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.code()).orElseThrow(() -> new RuntimeException("Código inválido."));
        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new RuntimeException("Código expirado.");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
        return ResponseEntity.ok("Senha redefinida!");
    }

    // --- DTOs LOCAIS ---
    public record LoginRequest(String login, String password) {}
    
    // [ATUALIZADO] Adicionei campos bloco, unidade e cpf aqui
    public record LoginResponse(
        String token, 
        String type, 
        String id, 
        String nome, 
        String email, 
        String role, 
        String tenantId,
        String bloco, 
        String unidade,
        String cpf
    ) {}
    
    public record ChangePasswordRequest(String oldPassword, String newPassword) {}
    public record ResetPasswordRequest(String code, String newPassword) {}
    
    public record RegisterCondoRequest(
        String planId, String condoName, String cnpj, Integer qtyUnits, Integer qtyBlocks, String secretKeyword,
        String nameSyndic, String emailSyndic, String cpfSyndic, String whatsappSyndic, String passwordSyndic,
        String paymentMethod, Map<String, String> creditCard,
        String couponCode 
    ) {}
}