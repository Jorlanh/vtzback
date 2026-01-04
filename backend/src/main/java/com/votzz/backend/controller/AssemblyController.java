package com.votzz.backend.controller;

import com.votzz.backend.domain.Assembly;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.Vote;
import com.votzz.backend.repository.AssemblyRepository;
import com.votzz.backend.repository.UserRepository; // Importante para buscar o Tenant
import com.votzz.backend.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assemblies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class AssemblyController {

    private final AssemblyRepository assemblyRepository;
    private final VoteRepository voteRepository; 
    private final UserRepository userRepository; // Injeção necessária para corrigir o erro 500

    @GetMapping
    public List<Assembly> getAll() {
        return assemblyRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> criarAssembleia(@RequestBody Assembly assembly) {
        try {
            // 1. Tenta identificar o usuário logado para definir o Condomínio (Tenant)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.isAuthenticated()) {
                // Assume que o principal é o email ou username
                String email = auth.getName(); 
                User user = userRepository.findByEmail(email).orElse(null);
                
                if (user != null && user.getTenant() != null) {
                    assembly.setTenant(user.getTenant());
                }
            }

            // 2. Lógica de Link de Vídeo
            if (assembly.getLinkVideoConferencia() == null || assembly.getLinkVideoConferencia().isEmpty()) {
                String salaId = UUID.randomUUID().toString();
                // Gera link Jitsi apenas se não for YouTube Live
                if (assembly.getYoutubeLiveUrl() == null || assembly.getYoutubeLiveUrl().isEmpty()) {
                    assembly.setLinkVideoConferencia("https://meet.jit.si/votzz-" + salaId);
                }
            }

            // 3. Garante status inicial
            if (assembly.getStatus() == null) {
                assembly.setStatus("AGENDADA");
            }
            
            // Garante datas se vierem nulas (fallback)
            if (assembly.getDataInicio() == null) assembly.setDataInicio(LocalDateTime.now());
            if (assembly.getDataFim() == null) assembly.setDataFim(LocalDateTime.now().plusDays(1));

            // 4. Salva no banco
            // Se o Tenant ainda for null aqui e o banco exigir, vai dar erro 500, 
            // mas o printStackTrace abaixo vai te mostrar exatamente o porquê no console do Java.
            Assembly savedAssembly = assemblyRepository.save(assembly);
            
            return ResponseEntity.ok(savedAssembly);

        } catch (Exception e) {
            e.printStackTrace(); // Imprime o erro real no terminal do VS Code
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Erro interno ao salvar assembleia.",
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assembly> getById(@PathVariable UUID id) {
        return assemblyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- VOTAR ---
    @PostMapping("/{id}/vote")
    @Transactional
    public ResponseEntity<?> votar(@PathVariable UUID id, @RequestBody VoteRequest request) {
        var assemblyOpt = assemblyRepository.findById(id);
        if (assemblyOpt.isEmpty()) return ResponseEntity.notFound().build();

        // Verifica voto duplicado
        if (voteRepository.existsByAssemblyIdAndUserId(id, request.userId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Você já votou nesta assembleia."));
        }

        Vote vote = new Vote();
        vote.setAssembly(assemblyOpt.get());
        
        User user = new User();
        user.setId(request.userId());
        vote.setUser(user);
        
        // Define o tenant do voto igual ao da assembleia (boa prática)
        if (assemblyOpt.get().getTenant() != null) {
            vote.setTenant(assemblyOpt.get().getTenant());
        }
        
        vote.setOptionId(request.optionId());
        vote.setHash(UUID.randomUUID().toString()); 
        vote.setFraction(new BigDecimal("0.0152")); 

        voteRepository.save(vote);
        return ResponseEntity.ok(Map.of("id", vote.getHash(), "message", "Voto confirmado"));
    }

    // --- ENCERRAR (ATA) ---
    @PatchMapping("/{id}/close") 
    public ResponseEntity<?> encerrar(@PathVariable UUID id) {
        return assemblyRepository.findById(id).map(assembly -> {
            assembly.setStatus("ENCERRADA"); // Define String direto para compatibilidade
            assemblyRepository.save(assembly);
            return ResponseEntity.ok(Map.of("message", "Assembleia encerrada com sucesso."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- DOSSIÊ ---
    @GetMapping("/{id}/dossier")
    public ResponseEntity<String> getDossier(@PathVariable UUID id) {
        return ResponseEntity.ok("Dossiê Jurídico da Assembleia " + id);
    }

    // DTO para o voto
    public record VoteRequest(String optionId, UUID userId) {}
}