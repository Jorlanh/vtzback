package com.votzz.backend.controller;

import com.votzz.backend.domain.Assembly;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.Vote;
import com.votzz.backend.repository.AssemblyRepository;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.repository.VoteRepository;
import com.votzz.backend.service.AuditService;
import com.votzz.backend.service.EmailService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assemblies")
@RequiredArgsConstructor
public class AssemblyController {

    private static final Logger logger = LoggerFactory.getLogger(AssemblyController.class);

    private final AssemblyRepository assemblyRepository;
    private final VoteRepository voteRepository; 
    private final UserRepository userRepository;
    private final AuditService auditService; 
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal User currentUser) {
        try {
            if (currentUser == null) return ResponseEntity.status(401).body("Não autorizado");

            if (currentUser.getRole() != null && currentUser.getRole().name().equals("ADMIN")) {
                return ResponseEntity.ok(assemblyRepository.findAll());
            }

            UUID tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null && currentUser.getTenant() != null) {
                tenantId = currentUser.getTenant().getId();
            }

            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Condomínio não identificado."));
            }

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

            UUID tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null && currentUser.getTenant() != null) {
                tenantId = currentUser.getTenant().getId();
            }

            if (tenantId == null) {
                return ResponseEntity.badRequest().body("Não foi possível identificar o condomínio.");
            }

            Tenant targetTenant = new Tenant();
            targetTenant.setId(tenantId);
            
            assembly.setTenant(targetTenant);

            if (assembly.getOptions() != null) {
                assembly.getOptions().forEach(option -> {
                    option.setTenant(targetTenant);
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
            auditService.log(currentUser, targetTenant, "CRIAR_ASSEMBLEIA", "Criou a assembleia: " + saved.getTitulo(), "ASSEMBLEIA");

            // Notificação automática na criação
            try {
                List<User> residents = userRepository.findByTenantId(tenantId);
                List<String> emails = residents.stream()
                        .map(User::getEmail)
                        .filter(email -> email != null && !email.isEmpty())
                        .collect(Collectors.toList());

                if (!emails.isEmpty()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String inicio = saved.getDataInicio().format(formatter);
                    String fim = saved.getDataFim() != null ? saved.getDataFim().format(formatter) : "Indefinido";
                    String periodo = "De " + inicio + " até " + fim;
                    
                    String link = "https://www.votzz.com.br/#/voting-room/" + saved.getId();
                    
                    // Pega o nome do Tenant se estiver carregado, ou usa padrão
                    String tName = currentUser.getTenant() != null ? currentUser.getTenant().getNome() : "Condomínio";
                    
                    emailService.sendNewAssemblyNotification(emails, saved.getTitulo(), saved.getDescription(), periodo, link, tName);
                }
            } catch (Exception e) {
                logger.error("Falha ao processar notificações automáticas: {}", e.getMessage());
            }
            
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

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> atualizarAssembleia(@PathVariable UUID id, @RequestBody Assembly updatedAssembly, @AuthenticationPrincipal User currentUser) {
        try {
            return assemblyRepository.findById(id).map(existingAssembly -> {
                existingAssembly.setTitulo(updatedAssembly.getTitulo());
                existingAssembly.setDescription(updatedAssembly.getDescription());
                existingAssembly.setDataInicio(updatedAssembly.getDataInicio());
                existingAssembly.setDataFim(updatedAssembly.getDataFim());
                existingAssembly.setStatus(updatedAssembly.getStatus());
                existingAssembly.setYoutubeLiveUrl(updatedAssembly.getYoutubeLiveUrl());
                existingAssembly.setLinkVideoConferencia(updatedAssembly.getLinkVideoConferencia());

                Assembly saved = assemblyRepository.save(existingAssembly);
                auditService.log(currentUser, existingAssembly.getTenant(), "EDITAR_ASSEMBLEIA", 
                    "Editou a assembleia: " + saved.getTitulo(), "ASSEMBLEIA");
                
                return ResponseEntity.ok(saved);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Erro ao editar assembleia: ", e);
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno ao editar assembleia."));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> excluirAssembleia(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        try {
            return assemblyRepository.findById(id).map(assembly -> {
                assemblyRepository.delete(assembly);
                auditService.log(currentUser, assembly.getTenant(), "EXCLUIR_ASSEMBLEIA", 
                    "Excluiu a assembleia: " + assembly.getTitulo(), "ASSEMBLEIA");
                return ResponseEntity.ok(Map.of("message", "Assembleia excluída com sucesso."));
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Erro ao excluir assembleia: ", e);
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno ao excluir assembleia."));
        }
    }

    // --- NOTIFICAÇÃO MANUAL (BOTÃO DE EMAIL) ---
    @PostMapping("/{id}/notify")
    public ResponseEntity<?> notificarMoradores(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        try {
            Assembly assembly = assemblyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Assembleia não encontrada"));

            UUID userTenantId = null;
            if (currentUser.getTenant() != null) userTenantId = currentUser.getTenant().getId();
            else if (currentUser.getTenants() != null && !currentUser.getTenants().isEmpty()) userTenantId = currentUser.getTenants().get(0).getId();

            if (userTenantId == null || (assembly.getTenant() != null && !userTenantId.equals(assembly.getTenant().getId()))) {
                return ResponseEntity.status(403).body("Permissão negada.");
            }

            List<User> residents = userRepository.findByTenantId(assembly.getTenant().getId());
            
            List<String> recipients = residents.stream()
                    .map(User::getEmail)
                    .filter(email -> email != null && !email.isEmpty() && email.contains("@"))
                    .collect(Collectors.toList());

            if (recipients.isEmpty()) {
                return ResponseEntity.badRequest().body("Nenhum morador com e-mail encontrado.");
            }

            // --- FORMATAÇÃO ---
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            String inicio = assembly.getDataInicio() != null 
                    ? assembly.getDataInicio().format(formatter) 
                    : "A definir";
            
            String fim = assembly.getDataFim() != null 
                    ? assembly.getDataFim().format(formatter) 
                    : "Indefinido";

            String periodoCompleto = String.format("De %s até %s", inicio, fim);

            // Link Oficial
            String link = "https://www.votzz.com.br/#/voting-room/" + assembly.getId();

            // Nome do Condomínio
            String tenantName = assembly.getTenant() != null ? assembly.getTenant().getNome() : "Seu Condomínio";

            // Chama o EmailService com os novos parâmetros
            emailService.sendNewAssemblyNotification(
                recipients, 
                assembly.getTitulo(), 
                assembly.getDescription(), 
                periodoCompleto, 
                link, 
                tenantName // <--- Passando o nome
            );

            auditService.log(currentUser, assembly.getTenant(), "NOTIFICAR_ASSEMBLEIA", 
                "Disparou notificação por e-mail para " + recipients.size() + " moradores.", "ASSEMBLEIA");

            return ResponseEntity.ok(Map.of(
                "message", "Disparo iniciado para " + recipients.size() + " moradores.",
                "count", recipients.size()
            ));

        } catch (Exception e) {
            logger.error("Erro ao notificar moradores: ", e);
            return ResponseEntity.internalServerError().body("Erro ao processar envio: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/vote")
    @Transactional
    public ResponseEntity<?> votar(@PathVariable UUID id, @RequestBody VoteRequest request, @AuthenticationPrincipal User currentUser) {
        var assemblyOpt = assemblyRepository.findById(id);
        if (assemblyOpt.isEmpty()) return ResponseEntity.notFound().build();
        Assembly assembly = assemblyOpt.get();

        if ("ENCERRADA".equalsIgnoreCase(assembly.getStatus()) || 
           (assembly.getDataFim() != null && LocalDateTime.now().isAfter(assembly.getDataFim()))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Votação encerrada."));
        }

        User voter = userRepository.findById(request.userId() != null ? request.userId() : currentUser.getId())
                .orElse(currentUser);

        List<String> unitsToVote = (request.units() != null && !request.units().isEmpty()) 
                ? request.units() 
                : List.of((voter.getBloco() != null ? voter.getBloco() + " " : "") + "unidade " + voter.getUnidade());

        int votosRegistrados = 0;
        StringBuilder receiptBuilder = new StringBuilder();

        for (String unidadeNome : unitsToVote) {
            String nomeLimpo = unidadeNome.trim();
            if (voteRepository.existsByAssemblyIdAndUnidade(id, nomeLimpo)) {
                logger.warn("Unidade {} já votou na assembleia {}", nomeLimpo, id);
                continue; 
            }

            Vote vote = new Vote();
            vote.setAssembly(assembly);
            vote.setUser(voter); 
            if (assembly.getTenant() != null) vote.setTenant(assembly.getTenant());
            
            vote.setOptionId(request.optionId());
            vote.setUnidade(nomeLimpo); 
            vote.setHash(UUID.randomUUID().toString()); 
            vote.setFraction(new BigDecimal("1.0")); 

            voteRepository.save(vote);
            votosRegistrados++;
            receiptBuilder.append(vote.getHash().substring(0, 8)).append("; ");
        }

        if (votosRegistrados == 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "As unidades selecionadas já votaram ou nenhuma foi selecionada."));
        }

        auditService.log(voter, assembly.getTenant(), "VOTO_REGISTRADO", 
            "Voto na pauta: " + assembly.getTitulo() + " (Unidades: " + String.join(", ", unitsToVote) + ")", "VOTACAO");

        return ResponseEntity.ok(Map.of(
            "id", receiptBuilder.toString(), 
            "message", votosRegistrados + " voto(s) computado(s) com sucesso!"
        ));
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

    public record VoteRequest(String optionId, UUID userId, List<String> units) {}
}