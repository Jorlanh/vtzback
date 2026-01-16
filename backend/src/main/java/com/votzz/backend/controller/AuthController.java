package com.votzz.backend.controller;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final AuthService authService; 
    private final TrustedDeviceRepository trustedDeviceRepository; // Repositório para dispositivos confiáveis
    
    // Instância do Google Authenticator
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // --- CONFIGURAÇÃO: DOMÍNIOS PERMITIDOS ---
    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
        "gmail.com", 
        "outlook.com", 
        "hotmail.com", 
        "yahoo.com", 
        "icloud.com", 
        "me.com",
        "codecloudcorp.com",
        "votzz.com"
    );

    // --- HELPER: Validação de Email ---
    private boolean isEmailAllowed(String email) {
        if (email == null || !email.contains("@")) return false;
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase().trim();
        return ALLOWED_EMAIL_DOMAINS.contains(domain);
    }

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

    // --- 1. LOGIN (LÓGICA DE MÚLTIPLOS PERFIS + 2FA + TRUST DEVICE) ---
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 1. Busca usuários
        List<User> candidates = userRepository.findAllByEmailOrCpf(request.login(), request.login());

        if (candidates.isEmpty()) {
            throw new RuntimeException("Usuário ou senha inválidos");
        }

        // 2. Valida Senha
        List<User> validUsers = candidates.stream()
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .collect(Collectors.toList());

        if (validUsers.isEmpty()) {
            throw new RuntimeException("Usuário ou senha inválidos");
        }

        User selectedUser = null;

        // 3. Seleção de Perfil (Multi-Tenant)
        if (request.selectedProfileId() != null && !request.selectedProfileId().isEmpty()) {
            selectedUser = validUsers.stream()
                    .filter(u -> u.getId().toString().equals(request.selectedProfileId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Perfil selecionado inválido."));
        } 
        else if (validUsers.size() == 1) {
            selectedUser = validUsers.get(0);
        } 
        else {
            // Retorna lista para o usuário escolher
            List<ProfileOption> options = validUsers.stream().map(u -> {
                String label = u.getNome();
                if (u.getUnidade() != null) {
                    label += " (Unid: " + u.getUnidade() + ")";
                }
                
                return new ProfileOption(
                    u.getId().toString(),
                    label,
                    u.getRole().name(),
                    u.getTenant() != null ? u.getTenant().getNome() : (u.getRole() == Role.ADMIN ? "Admin Votzz" : "Área do Parceiro")
                );
            }).toList();

            return ResponseEntity.ok(new LoginResponse(
                null, null, null, null, null, null, null, null, null, null, 
                true, // multipleProfiles = true
                false, 
                options
            ));
        }

        // 4. VERIFICAÇÃO 2FA (COM DISPOSITIVO CONFIÁVEL)
        if (Boolean.TRUE.equals(selectedUser.getIs2faEnabled())) {
            boolean isDeviceTrusted = false;

            // Verifica se o dispositivo já é confiável no banco
            if (request.deviceId() != null && !request.deviceId().isBlank()) {
                var trust = trustedDeviceRepository.findByUserIdAndDeviceIdentifier(selectedUser.getId(), request.deviceId());
                // Se existe e a data de expiração ainda é válida
                if (trust.isPresent() && trust.get().getExpiresAt().isAfter(LocalDateTime.now())) {
                    isDeviceTrusted = true;
                }
            }

            // Se NÃO é confiável, exige código
            if (!isDeviceTrusted) {
                // Se o código não veio, pede para o front
                if (request.code2fa() == null) {
                     return ResponseEntity.ok(new LoginResponse(
                        null, null, null, null, null, null, null, null, null, null, 
                        false, 
                        true, // requiresTwoFactor = TRUE
                        null
                    ));
                }
                
                // Valida o código
                boolean isCodeValid = gAuth.authorize(selectedUser.getSecret2fa(), request.code2fa());
                if (!isCodeValid) {
                    throw new RuntimeException("Código de autenticação inválido.");
                }

                // Se o código está certo e o usuário marcou "Confiar"
                if (request.trustDevice() && request.deviceId() != null) {
                    TrustedDevice device = trustedDeviceRepository
                        .findByUserIdAndDeviceIdentifier(selectedUser.getId(), request.deviceId())
                        .orElse(new TrustedDevice());
                    
                    device.setUser(selectedUser);
                    device.setDeviceIdentifier(request.deviceId());
                    device.setExpiresAt(LocalDateTime.now().plusDays(30)); // Validade de 30 dias
                    device.setCreatedAt(LocalDateTime.now());
                    
                    trustedDeviceRepository.save(device);
                }
            }
        }

        // 5. Login com Sucesso
        selectedUser.setLastSeen(LocalDateTime.now());
        userRepository.save(selectedUser);

        // ALTERAÇÃO: Passa o 'keepLogged' para o TokenService
        String token = tokenService.generateToken(selectedUser, request.keepLogged());

        return ResponseEntity.ok(new LoginResponse(
            token, "Bearer", selectedUser.getId().toString(), selectedUser.getNome(), selectedUser.getEmail(),
            selectedUser.getRole().name(), 
            selectedUser.getTenant() != null ? selectedUser.getTenant().getId().toString() : null,
            selectedUser.getBloco(), selectedUser.getUnidade(), selectedUser.getCpf(),
            false, 
            false, // requiresTwoFactor = FALSE (ou já passou)
            null
        ));
    }
    
    @PostMapping("/select-context")
    public ResponseEntity<LoginResponse> selectContext(@RequestBody Map<String, String> payload) {
        String userId = payload.get("userId");
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = tokenService.generateToken(user);
        
        return ResponseEntity.ok(new LoginResponse(
            token, "Bearer", user.getId().toString(), user.getNome(), user.getEmail(),
            user.getRole().name(), 
            user.getTenant() != null ? user.getTenant().getId().toString() : null,
            user.getBloco(), user.getUnidade(), user.getCpf(),
            false, false, null
        ));
    }

    // --- NOVOS ENDPOINTS 2FA ---

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponse> setup2FA(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        
        user.setSecret2fa(secret);
        user.setIs2faEnabled(false); 
        userRepository.save(user);

        String otpAuthURL = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL("Votzz", user.getEmail(), key);
        
        return ResponseEntity.ok(new TwoFactorSetupResponse(secret, otpAuthURL));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<String> confirm2FA(@RequestBody TwoFactorConfirmRequest request, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        if (user.getSecret2fa() == null) {
            throw new RuntimeException("Configuração de 2FA não iniciada.");
        }

        try {
            int code = Integer.parseInt(request.code());
            boolean isCodeValid = gAuth.authorize(user.getSecret2fa(), code);
            
            if (isCodeValid) {
                user.setIs2faEnabled(true);
                userRepository.save(user);
                return ResponseEntity.ok("Autenticação de dois fatores ativada com sucesso!");
            } else {
                return ResponseEntity.badRequest().body("Código inválido.");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("O código deve conter apenas números.");
        }
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<String> disable2FA(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        user.setIs2faEnabled(false);
        user.setSecret2fa(null);
        userRepository.save(user);
        return ResponseEntity.ok("2FA desativado.");
    }

    // --- 2. REGISTRO DE AFILIADO ---
    @PostMapping("/register-affiliate")
    @Transactional
    public ResponseEntity<?> registerAffiliate(@RequestBody AffiliateRegisterRequest request) {
        if (request.cpf() != null && !isValidCPF(request.cpf())) return ResponseEntity.badRequest().body(Map.of("message", "CPF inválido."));
        
        if (!isEmailAllowed(request.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Provedor de e-mail não permitido."));
        }

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

    @GetMapping("/validate-coupon")
    public ResponseEntity<BigDecimal> validateCoupon(@RequestParam String code) {
        return ResponseEntity.ok(authService.validateCoupon(code.toUpperCase()));
    }

    // --- 4. CADASTRO DE CONDOMÍNIO ---
    @PostMapping("/register-condo")
    public ResponseEntity<RegisterResponse> registerCondo(@RequestBody RegisterCondoRequest request) {
        if (request.cpfSyndic() != null && !isValidCPF(request.cpfSyndic())) {
            throw new RuntimeException("CPF do síndico inválido.");
        }

        if (!isEmailAllowed(request.emailSyndic())) {
            throw new RuntimeException("E-mail do síndico inválido. Domínio não permitido.");
        }

        return ResponseEntity.ok(authService.registerCondo(request));
    }

    // --- 5. CADASTRO DE MORADOR ---
    @PostMapping("/register-resident")
    @Transactional
    public ResponseEntity<String> registerResident(@RequestBody ResidentRegisterRequest request) {
        if (request.cpf() != null && !isValidCPF(request.cpf())) return ResponseEntity.badRequest().body("CPF inválido.");
        
        if (!isEmailAllowed(request.email())) {
            return ResponseEntity.badRequest().body("Provedor de e-mail não permitido.");
        }

        Tenant tenant = tenantRepository.findByCnpjOrNome(request.condoIdentifier())
            .orElseThrow(() -> new RuntimeException("Condomínio não encontrado."));
        
        if (!tenant.getSecretKeyword().equals(request.secretKeyword())) return ResponseEntity.badRequest().body("Palavra-chave incorreta.");
        
        List<UnitDTO> unitsToRegister;
        if (request.units() != null && !request.units().isEmpty()) {
            unitsToRegister = request.units();
        } else {
            unitsToRegister = List.of(new UnitDTO(request.bloco(), request.unidade()));
        }

        List<User> existingUsers = userRepository.findAllByEmailOrCpf(request.email(), request.cpf());
        
        for (UnitDTO unitDto : unitsToRegister) {
            boolean alreadyExists = existingUsers.stream()
                .anyMatch(u -> u.getTenant() != null 
                            && u.getTenant().getId().equals(tenant.getId()) 
                            && u.getUnidade() != null && u.getUnidade().equalsIgnoreCase(unitDto.unidade())
                            && u.getBloco() != null && u.getBloco().equalsIgnoreCase(unitDto.bloco()));
            
            if (alreadyExists) {
                return ResponseEntity.badRequest().body("A unidade " + unitDto.unidade() + " - " + unitDto.bloco() + " já está vinculada a este usuário.");
            }
        }
        
        for (UnitDTO unitDto : unitsToRegister) {
            User user = new User();
            user.setNome(request.nome());
            user.setEmail(request.email());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setCpf(request.cpf());
            user.setWhatsapp(request.whatsapp()); 
            user.setUnidade(unitDto.unidade());
            user.setBloco(unitDto.bloco()); 
            user.setRole(Role.MORADOR);
            user.setTenant(tenant);
            
            userRepository.save(user);
        }

        return ResponseEntity.ok("Cadastro realizado com " + unitsToRegister.size() + " unidade(s)!");
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<String> verify2FA(@RequestBody Map<String, String> payload, Principal principal) {
        return ResponseEntity.badRequest().body("Código inválido.");
    }

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