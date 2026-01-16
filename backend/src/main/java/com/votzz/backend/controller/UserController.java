package com.votzz.backend.controller;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    // --- CRIAR NOVO USUÁRIO ---
    @PostMapping
    @Transactional
    public ResponseEntity<?> createUser(@RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        if (currentUser.getTenant() == null) {
            return ResponseEntity.status(403).body("Você precisa estar vinculado a um condomínio.");
        }

        if (userRepository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Email já cadastrado.");
        }

        if (data.cpf() != null && !data.cpf().isEmpty()) {
            boolean cpfExists = userRepository.findAll().stream()
                    .anyMatch(u -> data.cpf().equals(u.getCpf()));
            if (cpfExists) return ResponseEntity.badRequest().body("CPF já cadastrado no sistema.");
        }

        User newUser = new User();
        newUser.setNome(data.nome());
        newUser.setEmail(data.email());
        newUser.setCpf(data.cpf());
        newUser.setWhatsapp(data.whatsapp());
        newUser.setUnidade(data.unidade());
        newUser.setBloco(data.bloco());
        newUser.setTenant(currentUser.getTenant());

        if ((currentUser.getRole() == Role.SINDICO || currentUser.getRole() == Role.ADM_CONDO) && data.role() != null) {
            try {
                newUser.setRole(Role.valueOf(data.role()));
            } catch (Exception e) {
                newUser.setRole(Role.MORADOR);
            }
        } else {
            newUser.setRole(Role.MORADOR);
        }

        if (data.password() != null && !data.password().isBlank()) {
            newUser.setPassword(passwordEncoder.encode(data.password()));
        } else {
            newUser.setPassword(passwordEncoder.encode("votzz123"));
        }

        userRepository.save(newUser);

        auditService.log(
                currentUser,
                currentUser.getTenant(),
                "CRIAR_USUARIO",
                "Criou o usuário " + newUser.getNome() + " (Unidade: " + newUser.getUnidade() + ")",
                "USUARIOS"
        );

        return ResponseEntity.ok("Usuário criado com sucesso!");
    }

    // --- ATUALIZAR USUÁRIO ---
    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        return processUpdate(id, data, currentUser);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateUserPut(@PathVariable UUID id, @RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        return processUpdate(id, data, currentUser);
    }

    private ResponseEntity<?> processUpdate(UUID id, UserDTO data, User currentUser) {
        var userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOptional.get();
        StringBuilder changes = new StringBuilder();

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(403).body("Ninguém pode alterar o Super Admin.");
        }

        // ────────────────────────────────────────────────
        // DEBUG: MOSTRA TODOS OS VALORES IMPORTANTES
        // ────────────────────────────────────────────────
        System.out.println("=== DEBUG processUpdate ===");
        System.out.println("ID do path: " + id);
        System.out.println("ID do usuário alvo (banco): " + user.getId());
        System.out.println("ID do currentUser (token): " + currentUser.getId());
        System.out.println("Email currentUser: " + currentUser.getEmail());
        System.out.println("Email usuário alvo: " + user.getEmail());
        System.out.println("Tenant currentUser: " + (currentUser.getTenant() != null ? currentUser.getTenant().getId() : "NULL"));
        System.out.println("Tenant usuário alvo: " + (user.getTenant() != null ? user.getTenant().getId() : "NULL"));

        // Auto-edição: permite se ID igual OU se email igual + tenant compatível
        boolean isEditingSelf = currentUser.getId().equals(user.getId()) ||
                (currentUser.getEmail().equalsIgnoreCase(user.getEmail()) &&
                        (currentUser.getTenant() == null || user.getTenant() == null ||
                                currentUser.getTenant().getId().equals(user.getTenant().getId())));

        System.out.println("isEditingSelf (com fallback por email): " + isEditingSelf);
        System.out.println("=== FIM DEBUG ===");

        // ────────────────────────────────────────────────
        // AUTORIZAÇÃO
        // ────────────────────────────────────────────────
        if (!isEditingSelf) {
            if (currentUser.getTenant() == null) {
                if (currentUser.getRole() != Role.ADMIN) {
                    return ResponseEntity.status(403).body("Você precisa estar vinculado a um condomínio para editar outros usuários.");
                }
            }
            if (user.getTenant() == null) {
                return ResponseEntity.status(403).body("Usuário alvo sem condomínio associado.");
            }
            if (!currentUser.getTenant().getId().equals(user.getTenant().getId())) {
                return ResponseEntity.status(403).body("Você não pode editar usuários de outro condomínio.");
            }
        }

        // Atualização de campos cadastrais
        if (data.email() != null && !data.email().isEmpty() && !data.email().equals(user.getEmail())) {
            user.setEmail(data.email());
            changes.append("Email alterado. ");
        }
        if (data.whatsapp() != null && !data.whatsapp().equals(user.getWhatsapp())) {
            user.setWhatsapp(data.whatsapp());
            changes.append("WhatsApp alterado. ");
        }
        if (data.unidade() != null && !data.unidade().equals(user.getUnidade())) {
            user.setUnidade(data.unidade());
            changes.append("Unidade mudou. ");
        }
        if (data.bloco() != null && !data.bloco().equals(user.getBloco())) {
            user.setBloco(data.bloco());
            changes.append("Bloco mudou. ");
        }

        // Permissão para trocar Role
        if (data.role() != null && (currentUser.getRole() == Role.SINDICO || currentUser.getRole() == Role.ADM_CONDO)) {
            try {
                Role newRole = Role.valueOf(data.role());
                if (newRole != Role.ADMIN && newRole != user.getRole()) {
                    user.setRole(newRole);
                    changes.append("Cargo para " + newRole + ". ");
                }
            } catch (Exception e) {
            }
        }

        // Lógica de replicação de senha
        if (data.password() != null && !data.password().isBlank()) {
            String encodedPassword = passwordEncoder.encode(data.password());

            if (user.getCpf() != null && !user.getCpf().isBlank() && user.getTenant() != null) {
                List<User> todasAsUnidades = userRepository.findAll().stream()
                        .filter(u -> user.getCpf().equals(u.getCpf()) &&
                                u.getTenant() != null &&
                                u.getTenant().getId().equals(user.getTenant().getId()))
                        .toList();

                for (User unidade : todasAsUnidades) {
                    unidade.setPassword(encodedPassword);
                    userRepository.save(unidade);
                }
                changes.append("SENHA ATUALIZADA EM TODAS AS UNIDADES. ");
            } else {
                user.setPassword(encodedPassword);
                changes.append("SENHA ALTERADA. ");
            }
        }

        userRepository.save(user);

        if (!changes.isEmpty()) {
            auditService.log(
                    currentUser,
                    user.getTenant(),
                    "EDITAR_USUARIO",
                    "Alterações em " + user.getNome() + ": " + changes.toString(),
                    "USUARIOS"
            );
        }

        return ResponseEntity.ok("Dados atualizados com sucesso.");
    }

    @GetMapping
    public ResponseEntity<List<User>> listAll(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getTenant() == null) {
            if (currentUser.getRole() == Role.ADMIN) return ResponseEntity.ok(userRepository.findAll());
            return ResponseEntity.ok(List.of());
        }
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getTenant() != null && u.getTenant().getId().equals(currentUser.getTenant().getId()))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/role")
    @Transactional
    public ResponseEntity<?> promoteUser(@PathVariable UUID id, @RequestBody java.util.Map<String, String> payload, @AuthenticationPrincipal User currentUser) {
        User user = userRepository.findById(id).orElseThrow();
        String newRoleStr = payload.get("role");

        if ("MANAGER".equals(newRoleStr) || "ADM_CONDO".equals(newRoleStr)) {
            user.setRole(Role.ADM_CONDO);
            userRepository.save(user);

            auditService.log(
                    currentUser,
                    user.getTenant(),
                    "PROMOVER_USUARIO",
                    "Promoveu " + user.getNome() + " a Admin",
                    "USUARIOS"
            );

            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    public record UserDTO(String nome, String email, String cpf, String whatsapp, String unidade, String bloco, String role, String password) {}
}