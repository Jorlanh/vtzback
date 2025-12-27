package com.votzz.backend.controller;

import com.votzz.backend.domain.Assembly;
import com.votzz.backend.domain.Vote;
import com.votzz.backend.repository.AssemblyRepository;
import com.votzz.backend.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assemblies")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AssemblyController {

    private final AssemblyRepository assemblyRepository;
    private final VoteService voteService;

    @GetMapping
    public List<Assembly> getAll() {
        return assemblyRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Assembly> criarAssembleia(@RequestBody Assembly assembly) {
        if (assembly.getLinkVideoConferencia() == null || assembly.getLinkVideoConferencia().isEmpty()) {
            String salaId = UUID.randomUUID().toString();
            assembly.setLinkVideoConferencia("https://meet.jit.si/votzz-" + salaId);
        }
        return ResponseEntity.ok(assemblyRepository.save(assembly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assembly> getById(@PathVariable String id) {
        try {
            UUID uuid = UUID.fromString(id);
            return assemblyRepository.findById(uuid)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<Vote> votar(@PathVariable String id, 
                                      @RequestParam UUID userId, 
                                      @RequestParam String opcao) {
        UUID assemblyUuid = UUID.fromString(id);
        return ResponseEntity.ok(voteService.registrarVoto(assemblyUuid, userId, opcao));
    }

    // ACRESCENTADO: ENCERRAMENTO DE ASSEMBLEIA (ATA)
    @PostMapping("/{id}/close")
    public ResponseEntity<Assembly> encerrar(@PathVariable String id) {
        UUID uuid = UUID.fromString(id);
        return assemblyRepository.findById(uuid).map(assembly -> {
            assembly.setStatus(Assembly.StatusAssembly.ENCERRADA);
            return ResponseEntity.ok(assemblyRepository.save(assembly));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ACRESCENTADO: DOWNLOAD DO DOSSIÊ (SIMULADO)
    @GetMapping("/{id}/dossier")
    public ResponseEntity<String> getDossier(@PathVariable String id) {
        return ResponseEntity.ok("Conteúdo do Dossiê Jurídico da Assembleia " + id);
    }
}