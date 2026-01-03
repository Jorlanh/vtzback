package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.*;
import com.votzz.backend.service.AuthService;
import com.votzz.backend.service.EmailService;
import com.votzz.backend.service.TokenService;
import com.votzz.backend.dto.AuthDTOs.*; 
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.security.Principal;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final AfiliadoRepository afiliadoRepository;
    private final AuthService authService; // Serviço Principal

    // --- HELPER: Validação de CPF ---
    private boolean isValidCPF(String cpf) {
        if (cpf == null) return false;
        cpf = cpf.replaceAll("\\D", ""); 
        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) return false; 
        
        int[] pesos1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
        try {
            int digito1 = calcularDigito(cpf.substring(0, 9), pesos1);
            int digito2 = calcularDigito(cpf.substring(0, 9) + digito1, pesos2);
            return cpf.equals(cpf.substring(0, 9) + digito1 + digito2);
        } catch (Exception e) { return false; }
    }

    private int calcularDigito(String str, int[] peso) {
        int soma = 0;
        for (int i = 0; i < str.length(); i++) {
            soma += Integer.parseInt(str.substring(i, i + 1)) * peso[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    // --- 1. LOGIN ---
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmailOrCpf(request.login(), request.login())
                .orElseThrow(() -> new RuntimeException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Usuário ou senha inválidos");
        }

        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        String token = tokenService.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(
            token, "Bearer", user.getId().toString(), user.getNome(), user.getEmail(),
            user.getRole().name(), 
            user.getTenant() != null ? user.getTenant().getId().toString() : null,
            user.getBloco(), user.getUnidade(), user.getCpf()
        ));
    }

    // --- 2. REGISTRO DE AFILIADO ---
    @PostMapping("/register-affiliate")
    @Transactional
    public ResponseEntity<?> registerAffiliate(@RequestBody AffiliateRegisterRequest request) {
        if (request.cpf() != null && !isValidCPF(request.cpf())) return ResponseEntity.badRequest().body(Map.of("message", "CPF inválido."));
        if (userRepository.findByEmail(request.email()).isPresent()) return ResponseEntity.badRequest().body(Map.of("message", "E-mail já cadastrado."));
        if (request.cpf() != null && userRepository.existsByCpf(request.cpf())) return ResponseEntity.badRequest().body(Map.of("message", "CPF já cadastrado."));

        User user = new User();
        user.setNome(request.nome());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCpf(request.cpf());
        user.setWhatsapp(request.whatsapp());
        user.setRole(Role.AFILIADO);
        userRepository.save(user);

        Afiliado afiliado = new Afiliado();
        afiliado.setUser(user);
        afiliado.setChavePix(request.chavePix());
        
        String codigoFinal = (request.codigoAfiliado() != null && !request.codigoAfiliado().isBlank()) 
            ? request.codigoAfiliado().trim().toUpperCase().replaceAll("\\s+", "")
            : request.nome().trim().split(" ")[0].toUpperCase() + new Random().nextInt(1000);
            
        if (afiliadoRepository.existsByCodigoRef(codigoFinal)) return ResponseEntity.badRequest().body(Map.of("message", "Código em uso."));
        
        afiliado.setCodigoRef(codigoFinal);
        afiliadoRepository.save(afiliado);
        return ResponseEntity.ok(Map.of("message", "Afiliado cadastrado!"));
    }

    // --- 3. VALIDAÇÃO DE CUPOM (ATUALIZADO PARA SUPORTAR O BOTÃO 'APLICAR') ---
    @GetMapping("/validate-coupon")
    public ResponseEntity<BigDecimal> validateCoupon(@RequestParam String code) {
        // Usa a lógica centralizada no AuthService
        return ResponseEntity.ok(authService.validateCoupon(code.toUpperCase()));
    }

    // --- 4. CADASTRO DE CONDOMÍNIO ---
    @PostMapping("/register-condo")
    public ResponseEntity<RegisterResponse> registerCondo(@RequestBody RegisterCondoRequest request) {
        if (request.cpfSyndic() != null && !isValidCPF(request.cpfSyndic())) {
            throw new RuntimeException("CPF do síndico inválido.");
        }
        return ResponseEntity.ok(authService.registerCondo(request));
    }

    // --- 5. CADASTRO DE MORADOR ---
    @PostMapping("/register-resident")
    @Transactional
    public ResponseEntity<String> registerResident(@RequestBody ResidentRegisterRequest request) {
        if (request.cpf() != null && !isValidCPF(request.cpf())) return ResponseEntity.badRequest().body("CPF inválido.");
        Tenant tenant = tenantRepository.findByCnpjOrNome(request.condoIdentifier()).orElseThrow(() -> new RuntimeException("Condomínio não encontrado."));
        if (!tenant.getSecretKeyword().equals(request.secretKeyword())) return ResponseEntity.badRequest().body("Palavra-chave incorreta.");
        if (userRepository.findByEmail(request.email()).isPresent()) return ResponseEntity.badRequest().body("E-mail já cadastrado.");
        if (request.cpf() != null && userRepository.existsByCpf(request.cpf())) return ResponseEntity.badRequest().body("CPF já cadastrado.");

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

        return ResponseEntity.ok("Cadastro realizado!");
    }

    // --- 6. 2FA ---
    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setup2FA(Principal principal) {
        return ResponseEntity.ok(Map.of("message", "Setup 2FA iniciado."));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<String> verify2FA(@RequestBody Map<String, String> payload, Principal principal) {
        return ResponseEntity.badRequest().body("Código inválido.");
    }

    // --- OUTROS ---
    @PatchMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
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
        return ResponseEntity.ok("Código enviado.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.code()).orElseThrow(() -> new RuntimeException("Código inválido."));
        if (resetToken.isExpired()) throw new RuntimeException("Código expirado.");
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
        return ResponseEntity.ok("Senha redefinida!");
    }
}