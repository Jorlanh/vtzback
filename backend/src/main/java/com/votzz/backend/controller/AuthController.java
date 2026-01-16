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
import java.util.ArrayList;
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
    private final TrustedDeviceRepository trustedDeviceRepository; 
    
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
        "gmail.com", "outlook.com", "hotmail.com", "yahoo.com", "icloud.com", "me.com", "codecloudcorp.com", "votzz.com"
    );

    private boolean isEmailAllowed(String email) {
        if (email == null || !email.contains("@")) return false;
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase().trim();
        return ALLOWED_EMAIL_DOMAINS.contains(domain);
    }

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

    // --- 1. LOGIN (LÓGICA UNIFICADA & CORRIGIDA PARA MULTI-TENANT) ---
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

        // 3. Verifica se um perfil específico foi selecionado
        if (request.selectedProfileId() != null && !request.selectedProfileId().isEmpty()) {
            String selectedId = request.selectedProfileId();
            String targetTenantId = null;

            // Suporte a ID composto "userID:tenantID"
            if (selectedId.contains(":")) {
                String[] parts = selectedId.split(":");
                selectedId = parts[0];
                targetTenantId = parts[1];
            }

            String finalSelectedId = selectedId;
            selectedUser = validUsers.stream()
                    .filter(u -> u.getId().toString().equals(finalSelectedId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Perfil selecionado inválido."));

            // Se veio um tenantID no combo, define esse tenant como o ativo para a sessão
            if (targetTenantId != null) {
                String tId = targetTenantId;
                Tenant activeTenant = selectedUser.getTenants().stream()
                    .filter(t -> t.getId().toString().equals(tId))
                    .findFirst()
                    .orElse(selectedUser.getTenant()); // Fallback
                
                selectedUser.setTenant(activeTenant);
            }
        } 
        else {
            // --- CONSTRUÇÃO DO LEQUE DE OPÇÕES ---
            // Verifica se precisamos mostrar o leque.
            // Precisamos se:
            // A) Tem mais de 1 usuário válido (Ex: Conta Afiliado + Conta Morador separadas)
            // B) OU Tem 1 usuário, mas ele tem múltiplos condomínios na lista (Ex: Síndico Profissional)
            
            boolean hasMultipleOptions = validUsers.size() > 1;
            if (!hasMultipleOptions && !validUsers.isEmpty()) {
                User u = validUsers.get(0);
                if (u.getTenants() != null && u.getTenants().size() > 1) {
                    hasMultipleOptions = true;
                }
            }

            if (hasMultipleOptions) {
                List<ProfileOption> options = new ArrayList<>();

                for (User u : validUsers) {
                    String userLabel = u.getNome();
                    if (u.getUnidade() != null && !u.getUnidade().isEmpty()) {
                        userLabel += " (Unid: " + u.getUnidade() + ")";
                    }

                    // Se for Afiliado ou Admin, ou não tiver lista de tenants, adiciona opção simples
                    if (u.getRole() == Role.AFILIADO || u.getRole() == Role.ADMIN || u.getTenants().isEmpty()) {
                        String contextName = "Perfil Global";
                        if (u.getRole() == Role.AFILIADO) contextName = "Painel de Afiliado";
                        if (u.getRole() == Role.ADMIN) contextName = "Administração Votzz";
                        if (u.getTenant() != null && u.getRole() != Role.AFILIADO && u.getRole() != Role.ADMIN) contextName = u.getTenant().getNome();

                        options.add(new ProfileOption(
                            u.getId().toString(),
                            u.getRole().name(),
                            contextName,
                            userLabel
                        ));
                    } 
                    else {
                        // Se tiver lista de tenants (Síndico/Morador Multi-Condo), cria uma opção para cada
                        for (Tenant t : u.getTenants()) {
                            // ID COMPOSTO: userID:tenantID
                            String compositeId = u.getId().toString() + ":" + t.getId().toString();
                            
                            options.add(new ProfileOption(
                                compositeId,
                                u.getRole().name(),
                                t.getNome(), // Nome do Condomínio Específico
                                userLabel
                            ));
                        }
                    }
                }

                return ResponseEntity.ok(new LoginResponse(true, options));
            }
            
            // Se só tem 1 opção e nenhum tenant extra, segue direto
            selectedUser = validUsers.get(0);
        }

        // 4. VERIFICAÇÃO 2FA (Lógica mantida)
        if (Boolean.TRUE.equals(selectedUser.getIs2faEnabled())) {
            boolean isDeviceTrusted = false;
            if (request.deviceId() != null && !request.deviceId().isBlank()) {
                var trust = trustedDeviceRepository.findByUserIdAndDeviceIdentifier(selectedUser.getId(), request.deviceId());
                if (trust.isPresent() && trust.get().getExpiresAt().isAfter(LocalDateTime.now())) {
                    isDeviceTrusted = true;
                }
            }
            if (!isDeviceTrusted) {
                if (request.code2fa() == null) {
                      return ResponseEntity.ok(new LoginResponse(
                        null, null, null, null, null, null, null, null, null, null, null, true, false, null
                    ));
                }
                boolean isCodeValid = gAuth.authorize(selectedUser.getSecret2fa(), request.code2fa());
                if (!isCodeValid) throw new RuntimeException("Código de autenticação inválido.");

                if (request.trustDevice() && request.deviceId() != null) {
                    TrustedDevice device = trustedDeviceRepository.findByUserIdAndDeviceIdentifier(selectedUser.getId(), request.deviceId()).orElse(new TrustedDevice());
                    device.setUser(selectedUser);
                    device.setDeviceIdentifier(request.deviceId());
                    device.setExpiresAt(LocalDateTime.now().plusDays(30));
                    device.setCreatedAt(LocalDateTime.now());
                    trustedDeviceRepository.save(device);
                }
            }
        }

        // 5. Login Finalizado
        selectedUser.setLastSeen(LocalDateTime.now());
        // Não salvamos o tenant setado transiently (setTenant) para não bagunçar o banco, 
        // o save aqui é só pro lastSeen. O tokenService usará o tenant que setamos na memória.
        userRepository.save(selectedUser);

        String token = tokenService.generateToken(selectedUser, request.keepLogged());

        return ResponseEntity.ok(new LoginResponse(
            token, 
            "Bearer", 
            selectedUser.getId().toString(), 
            selectedUser.getNome(), 
            selectedUser.getEmail(),
            selectedUser.getRole().name(), 
            selectedUser.getTenant() != null ? selectedUser.getTenant().getId().toString() : null,
            selectedUser.getTenant() != null ? selectedUser.getTenant().getNome() : "Sem Condomínio",
            selectedUser.getBloco(), 
            selectedUser.getUnidade(), 
            selectedUser.getCpf(),
            false, 
            false, 
            null
        ));
    }
    
    // Método select-context mantido para compatibilidade, mas o fluxo principal acima já resolve o leque
    @PostMapping("/select-context")
    public ResponseEntity<LoginResponse> selectContext(@RequestBody Map<String, String> payload) {
        String userIdRaw = payload.get("userId");
        String userId = userIdRaw;
        String tenantId = null;
        
        if (userIdRaw.contains(":")) {
            String[] parts = userIdRaw.split(":");
            userId = parts[0];
            tenantId = parts[1];
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (tenantId != null) {
            String tId = tenantId;
            Tenant activeTenant = user.getTenants().stream()
                .filter(t -> t.getId().toString().equals(tId))
                .findFirst()
                .orElse(user.getTenant());
            user.setTenant(activeTenant);
        }

        String token = tokenService.generateToken(user);
        
        return ResponseEntity.ok(new LoginResponse(
            token, "Bearer", user.getId().toString(), user.getNome(), user.getEmail(),
            user.getRole().name(), 
            user.getTenant() != null ? user.getTenant().getId().toString() : null,
            user.getTenant() != null ? user.getTenant().getNome() : null,
            user.getBloco(), user.getUnidade(), user.getCpf(),
            false, false, null
        ));
    }

    // --- (MÉTODOS 2FA, REGISTRO, ETC. MANTIDOS IGUAIS ABAIXO) ---
    // ... Mantenha o restante do código que você já tem para register, forgotPassword, etc. ...
    
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
        if (user.getSecret2fa() == null) throw new RuntimeException("Configuração de 2FA não iniciada.");
        try {
            int code = Integer.parseInt(request.code());
            if (gAuth.authorize(user.getSecret2fa(), code)) {
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

    @PostMapping("/register-affiliate")
    @Transactional
    public ResponseEntity<?> registerAffiliate(@RequestBody AffiliateRegisterRequest request) {
        if (request.cpf() != null && !isValidCPF(request.cpf())) return ResponseEntity.badRequest().body(Map.of("message", "CPF inválido."));
        if (!isEmailAllowed(request.email())) return ResponseEntity.badRequest().body(Map.of("message", "Provedor de e-mail não permitido."));

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

    @PostMapping("/register-condo")
    public ResponseEntity<RegisterResponse> registerCondo(@RequestBody RegisterCondoRequest request) {
        if (request.cpfSyndic() != null && !isValidCPF(request.cpfSyndic())) throw new RuntimeException("CPF do síndico inválido.");
        if (!isEmailAllowed(request.emailSyndic())) throw new RuntimeException("E-mail do síndico inválido. Domínio não permitido.");
        return ResponseEntity.ok(authService.registerCondo(request));
    }

    @PostMapping("/register-resident")
    @Transactional
    public ResponseEntity<String> registerResident(@RequestBody ResidentRegisterRequest request) {
        if (request.cpf() != null && !isValidCPF(request.cpf())) return ResponseEntity.badRequest().body("CPF inválido.");
        if (!isEmailAllowed(request.email())) return ResponseEntity.badRequest().body("Provedor de e-mail não permitido.");

        Tenant tenant = tenantRepository.findByCnpjOrNome(request.condoIdentifier())
            .orElseThrow(() -> new RuntimeException("Condomínio não encontrado."));
        
        if (!tenant.getSecretKeyword().equals(request.secretKeyword())) return ResponseEntity.badRequest().body("Palavra-chave incorreta.");
        
        List<UnitDTO> unitsToRegister;
        if (request.units() != null && !request.units().isEmpty()) unitsToRegister = request.units();
        else unitsToRegister = List.of(new UnitDTO(request.bloco(), request.unidade()));

        List<User> existingUsers = userRepository.findAllByEmailOrCpf(request.email(), request.cpf());
        for (UnitDTO unitDto : unitsToRegister) {
            boolean alreadyExists = existingUsers.stream()
                .anyMatch(u -> u.getTenant() != null 
                            && u.getTenant().getId().equals(tenant.getId()) 
                            && u.getUnidade() != null && u.getUnidade().equalsIgnoreCase(unitDto.unidade())
                            && u.getBloco() != null && u.getBloco().equalsIgnoreCase(unitDto.bloco()));
            if (alreadyExists) return ResponseEntity.badRequest().body("A unidade " + unitDto.unidade() + " - " + unitDto.bloco() + " já está vinculada a este usuário.");
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