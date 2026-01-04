package com.votzz.backend.controller;

import com.votzz.backend.domain.Assembly;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.Vote;
import com.votzz.backend.repository.AssemblyRepository;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.repository.VoteRepository;
import com.votzz.backend.service.AuditService; 
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final UserRepository userRepository;
    private final AuditService auditService; 

    @GetMapping
    public List<Assembly> getAll() {
        return assemblyRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> criarAssembleia(@RequestBody Assembly assembly, @AuthenticationPrincipal User currentUser) {
        try {
            if (currentUser != null && currentUser.getTenant() != null) {
                assembly.setTenant(currentUser.getTenant());
            } else {
                return ResponseEntity.badRequest().body("Usuário sem condomínio.");
            }

            if (assembly.getLinkVideoConferencia() == null || assembly.getLinkVideoConferencia().isEmpty()) {
                String salaId = UUID.randomUUID().toString();
                if (assembly.getYoutubeLiveUrl() == null || assembly.getYoutubeLiveUrl().isEmpty()) {
                    assembly.setLinkVideoConferencia("https://meet.jit.si/votzz-" + salaId);
                }
            }

            if (assembly.getStatus() == null) assembly.setStatus("AGENDADA");
            if (assembly.getDataInicio() == null) assembly.setDataInicio(LocalDateTime.now());
            if (assembly.getDataFim() == null) assembly.setDataFim(LocalDateTime.now().plusDays(1));

            Assembly saved = assemblyRepository.save(assembly);
            
            // --- CORREÇÃO: Passando saved.getTenant() como targetTenant ---
            auditService.log(
                currentUser, 
                saved.getTenant(), // targetTenant
                "CRIAR_ASSEMBLEIA", 
                "Criou a assembleia: " + saved.getTitulo(), 
                "ASSEMBLEIA"
            );
            
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao criar assembleia.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assembly> getById(@PathVariable UUID id) {
        return assemblyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/vote")
    @Transactional
    public ResponseEntity<?> votar(@PathVariable UUID id, @RequestBody VoteRequest request, @AuthenticationPrincipal User currentUser) {
        var assemblyOpt = assemblyRepository.findById(id);
        if (assemblyOpt.isEmpty()) return ResponseEntity.notFound().build();

        if (voteRepository.existsByAssemblyIdAndUserId(id, request.userId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Você já votou nesta assembleia."));
        }

        Vote vote = new Vote();
        vote.setAssembly(assemblyOpt.get());
        
        User voter = userRepository.findById(request.userId()).orElse(currentUser);
        vote.setUser(voter);
        
        if (assemblyOpt.get().getTenant() != null) {
            vote.setTenant(assemblyOpt.get().getTenant());
        }
        
        vote.setOptionId(request.optionId());
        vote.setHash(UUID.randomUUID().toString()); 
        vote.setFraction(new BigDecimal("0.0152")); 

        voteRepository.save(vote);

        String detalheVoto = "Voto registrado na pauta '" + assemblyOpt.get().getTitulo() + 
                             "'. Opção: " + request.optionId() + 
                             ". Morador: " + voter.getNome() + 
                             " (Bl: " + voter.getBloco() + ", Ap: " + voter.getUnidade() + ")";
                             
        // --- CORREÇÃO: Passando assemblyOpt.get().getTenant() como targetTenant ---
        auditService.log(
            voter, 
            assemblyOpt.get().getTenant(), // targetTenant
            "VOTO_REGISTRADO", 
            detalheVoto, 
            "VOTACAO"
        );

        return ResponseEntity.ok(Map.of("id", vote.getHash(), "message", "Voto confirmado"));
    }

    @PatchMapping("/{id}/close") 
    public ResponseEntity<?> encerrar(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return assemblyRepository.findById(id).map(assembly -> {
            assembly.setStatus("ENCERRADA"); 
            assemblyRepository.save(assembly);
            
            // --- CORREÇÃO: Passando assembly.getTenant() como targetTenant ---
            auditService.log(
                currentUser, 
                assembly.getTenant(), // targetTenant
                "ENCERRAR_ASSEMBLEIA", 
                "Encerrou a assembleia: " + assembly.getTitulo(), 
                "ASSEMBLEIA"
            );

            return ResponseEntity.ok(Map.of("message", "Assembleia encerrada com sucesso."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/dossier")
    public ResponseEntity<String> getDossier(@PathVariable UUID id) {
        return ResponseEntity.ok("Dossiê Jurídico da Assembleia " + id);
    }

    public record VoteRequest(String optionId, UUID userId) {}
}