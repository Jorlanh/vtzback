package com.votzz.backend.controller;

import com.votzz.backend.domain.Assembly;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.Vote;
import com.votzz.backend.repository.AssemblyRepository;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.repository.VoteRepository;
import com.votzz.backend.service.AuditService;
import com.votzz.backend.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AssemblyController.class);

    private final AssemblyRepository assemblyRepository;
    private final VoteRepository voteRepository; 
    private final UserRepository userRepository;
    private final AuditService auditService; 

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal User currentUser) {
        try {
            if (currentUser == null) {
                logger.warn("Tentativa de acesso sem usuário autenticado.");
                return ResponseEntity.status(401).body("Não autorizado");
            }

            // Prioridade 1: ADMIN vê tudo
            if (currentUser.getRole() != null && currentUser.getRole().name().equals("ADMIN")) {
                return ResponseEntity.ok(assemblyRepository.findAll());
            }

            // Prioridade 2: Buscar Tenant ID do Contexto (Header X-Tenant-ID)
            UUID tenantId = TenantContext.getTenant();
            
            // Prioridade 3: Fallback para o Tenant do objeto User
            if (tenantId == null && currentUser.getTenant() != null) {
                tenantId = currentUser.getTenant().getId();
            }

            if (tenantId == null) {
                logger.error("ERRO 400: Tenant ID não encontrado para o usuário: {}", currentUser.getEmail());
                return ResponseEntity.badRequest().body(Map.of("error", "Condomínio não identificado. Faça login novamente."));
            }

            logger.info("Listando assembleias para o Tenant: {}", tenantId);
            List<Assembly> lista = assemblyRepository.findByTenantId(tenantId);
            return ResponseEntity.ok(lista);

        } catch (Exception e) {
            logger.error("Erro interno ao listar assembleias: ", e);
            return ResponseEntity.status(500).body("Erro interno: " + e.getMessage());
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> criarAssembleia(@RequestBody Assembly assembly, @AuthenticationPrincipal User currentUser) {
        try {
            if (currentUser == null) return ResponseEntity.status(401).body("Usuário não autenticado.");

            UUID tenantId = TenantContext.getTenant();
            if (tenantId == null && currentUser.getTenant() != null) tenantId = currentUser.getTenant().getId();

            if (tenantId == null) {
                return ResponseEntity.badRequest().body("Não foi possível identificar o condomínio.");
            }

            // Vincula o Tenant do usuário à assembleia
            assembly.setTenant(currentUser.getTenant());

            if (assembly.getOptions() != null) {
                assembly.getOptions().forEach(option -> {
                    option.setTenant(assembly.getTenant());
                    option.setAssembly(assembly);
                });
            }

            if (assembly.getLinkVideoConferencia() == null || assembly.getLinkVideoConferencia().isEmpty()) {
                if (assembly.getYoutubeLiveUrl() == null || assembly.getYoutubeLiveUrl().isEmpty()) {
                    assembly.setLinkVideoConferencia("https://meet.jit.si/votzz-" + UUID.randomUUID().toString().substring(0, 8));
                }
            }

            if (assembly.getStatus() == null) assembly.setStatus("AGENDADA");
            if (assembly.getDataInicio() == null) assembly.setDataInicio(LocalDateTime.now());
            if (assembly.getDataFim() == null) assembly.setDataFim(LocalDateTime.now().plusDays(2));

            Assembly saved = assemblyRepository.save(assembly);
            auditService.log(currentUser, saved.getTenant(), "CRIAR_ASSEMBLEIA", "Criou a assembleia: " + saved.getTitulo(), "ASSEMBLEIA");
            
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Erro ao criar assembleia: ", e);
            return ResponseEntity.internalServerError().body("Erro ao salvar: " + e.getMessage());
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

        UUID voterId = request.userId() != null ? request.userId() : currentUser.getId();
        if (voteRepository.existsByAssemblyIdAndUserId(id, voterId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Você já votou nesta assembleia."));
        }

        Vote vote = new Vote();
        vote.setAssembly(assemblyOpt.get());
        User voter = userRepository.findById(voterId).orElse(currentUser);
        vote.setUser(voter);
        
        if (assemblyOpt.get().getTenant() != null) {
            vote.setTenant(assemblyOpt.get().getTenant());
        }
        
        vote.setOptionId(request.optionId());
        vote.setHash(UUID.randomUUID().toString()); 
        vote.setFraction(new BigDecimal("1.0")); 

        voteRepository.save(vote);
        auditService.log(voter, assemblyOpt.get().getTenant(), "VOTO_REGISTRADO", "Voto na pauta: " + assemblyOpt.get().getTitulo(), "VOTACAO");

        return ResponseEntity.ok(Map.of("id", vote.getHash(), "message", "Voto confirmado"));
    }

    @PatchMapping("/{id}/close") 
    public ResponseEntity<?> encerrar(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return assemblyRepository.findById(id).map(assembly -> {
            assembly.setStatus("ENCERRADA"); 
            assemblyRepository.save(assembly);
            auditService.log(currentUser, assembly.getTenant(), "ENCERRAR_ASSEMBLEIA", "Encerrou a assembleia: " + assembly.getTitulo(), "ASSEMBLEIA");
            return ResponseEntity.ok(Map.of("message", "Assembleia encerrada com sucesso."));
        }).orElse(ResponseEntity.notFound().build());
    }

    public record VoteRequest(String optionId, UUID userId) {}
}