package com.votzz.backend.controller;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.TenantRepository;
import com.votzz.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class TenantController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    // Endpoint público para listar condomínios no dropdown de cadastro
    @GetMapping("/public-list")
    public List<TenantDTO> listPublic() {
        return tenantRepository.findAll().stream()
                .map(t -> new TenantDTO(t.getId(), t.getNome()))
                .toList();
    }

    // Endpoint para o SÍNDICO atualizar a palavra-chave
    @PatchMapping("/secret-keyword")
    public ResponseEntity<String> updateSecretKeyword(@RequestBody Map<String, String> payload, Principal principal) {
        // 1. Identifica quem está logado
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Valida se é SÍNDICO (Somente ele pode mudar)
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Apenas o Síndico pode alterar a palavra-chave.");
        }

        // 3. Atualiza o Condomínio
        Tenant tenant = user.getTenant();
        String newKeyword = payload.get("secretKeyword");
        
        if (newKeyword == null || newKeyword.length() < 4) {
            return ResponseEntity.badRequest().body("A palavra-chave deve ter pelo menos 4 caracteres.");
        }

        tenant.setSecretKeyword(newKeyword);
        tenantRepository.save(tenant);

        return ResponseEntity.ok("Palavra-chave de segurança atualizada com sucesso!");
    }

    public record TenantDTO(UUID id, String nome) {}
}