package com.votzz.backend.controller;

import com.votzz.backend.domain.PasswordResetToken;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.PasswordResetTokenRepository;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.service.EmailService;
import com.votzz.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;

    // --- 1. Login ---
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Usuário ou senha inválidos");
        }

        String token = tokenService.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(
            token,
            user.getNome(),
            user.getEmail(),
            user.getRole().name(),
            user.getTenant() != null ? user.getTenant().getId().toString() : null
        ));
    }

    // --- 2. Troca de Senha (Usuário Logado) ---
    @PatchMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordRequest request,
            Principal principal // Pega o usuário do Token JWT automaticamente
    ) {
        // Busca usuário pelo e-mail do token
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Valida senha atual
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("A senha atual está incorreta.");
        }

        // Salva nova senha
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("Senha alterada com sucesso!");
    }

    // --- 3. Esqueci Minha Senha (Gera código) ---
    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("E-mail não encontrado."));

        // Remove tokens antigos se existirem
        resetTokenRepository.findByUser(user).ifPresent(resetTokenRepository::delete);

        // Gera código numérico de 6 dígitos
        String code = String.format("%06d", new Random().nextInt(999999));

        // Salva no banco com validade de 15 min
        PasswordResetToken token = new PasswordResetToken(user, code);
        resetTokenRepository.save(token);

        // Envia por e-mail
        emailService.sendResetToken(user.getEmail(), code);

        return ResponseEntity.ok("Código de verificação enviado para o e-mail: " + user.getEmail());
    }

    // --- 4. Resetar Senha (Usa código) ---
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.code())
                .orElseThrow(() -> new RuntimeException("Código inválido ou inexistente."));

        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new RuntimeException("Código expirado. Solicite um novo.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Queima o token
        resetTokenRepository.delete(resetToken);

        return ResponseEntity.ok("Senha redefinida com sucesso! Faça login novamente.");
    }

    // --- DTOs ---
    public record LoginRequest(String email, String password) {}
    
    public record LoginResponse(
        String token, 
        String nome, 
        String email, 
        String role, 
        String tenantId
    ) {}

    public record ChangePasswordRequest(String oldPassword, String newPassword) {}

    public record ResetPasswordRequest(String code, String newPassword) {}
}