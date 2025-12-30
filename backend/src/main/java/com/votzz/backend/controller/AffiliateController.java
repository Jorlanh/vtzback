package com.votzz.backend.controller;

import com.votzz.backend.domain.Afiliado;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.AfiliadoRepository;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.service.AffiliateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/afiliado")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AffiliateController {

    private final UserRepository userRepository;
    private final AfiliadoRepository afiliadoRepository;
    private final AffiliateService affiliateService;
    private final PasswordEncoder passwordEncoder;

    // --- DASHBOARD DE AFILIADO ---
    @GetMapping("/{afiliadoId}/dashboard")
    public ResponseEntity<AffiliateService.DashboardDTO> getDashboard(@PathVariable UUID afiliadoId) {
        return ResponseEntity.ok(affiliateService.getDashboard(afiliadoId));
    }
    
    // --- FORÇAR PAGAMENTOS (DEV/ADMIN) ---
    @PostMapping("/forcar-pagamentos")
    public ResponseEntity<String> forcePayments() {
        affiliateService.processarPagamentosAutomaticos();
        return ResponseEntity.ok("Processamento de pagamentos iniciado em background.");
    }

    // --- ATUALIZAR PERFIL DO AFILIADO (PIX, DADOS PESSOAIS) ---
    @PatchMapping("/profile")
    @Transactional
    public ResponseEntity<String> updateProfile(@RequestBody UpdateAffiliateRequest request, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // O erro de compilação ocorria aqui porque o método não existia no repositório
        Afiliado afiliado = afiliadoRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Perfil de afiliado não encontrado."));

        // 1. Atualizar Dados Básicos (User)
        if (request.whatsapp() != null && !request.whatsapp().isBlank()) {
            user.setWhatsapp(request.whatsapp());
        }
        
        // Troca de Email com verificação
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.email()).isPresent()) {
                return ResponseEntity.badRequest().body("Email já em uso.");
            }
            user.setEmail(request.email());
        }

        // Troca de Senha (Opcional no mesmo form)
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        // 2. Atualizar Dados Específicos (Afiliado - PIX)
        if (request.chavePix() != null && !request.chavePix().isBlank()) {
            afiliado.setChavePix(request.chavePix());
        }

        userRepository.save(user);
        afiliadoRepository.save(afiliado);

        return ResponseEntity.ok("Perfil de parceiro atualizado com sucesso!");
    }

    public record UpdateAffiliateRequest(String email, String whatsapp, String password, String chavePix) {}
}