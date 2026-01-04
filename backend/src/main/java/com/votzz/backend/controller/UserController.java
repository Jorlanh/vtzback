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
             if(cpfExists) return ResponseEntity.badRequest().body("CPF já cadastrado no sistema.");
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
            try { newUser.setRole(Role.valueOf(data.role())); } catch (Exception e) { newUser.setRole(Role.MORADOR); }
        } else {
            newUser.setRole(Role.MORADOR);
        }

        if (data.password() != null && !data.password().isBlank()) {
            newUser.setPassword(passwordEncoder.encode(data.password()));
        } else {
            newUser.setPassword(passwordEncoder.encode("votzz123"));
        }

        userRepository.save(newUser);

        // --- CORREÇÃO: Passando currentUser.getTenant() como targetTenant ---
        auditService.log(
            currentUser, 
            currentUser.getTenant(), // targetTenant
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

        if (user.getRole() == Role.ADMIN) return ResponseEntity.status(403).body("Ninguém pode alterar o Super Admin.");

        // Validação de Tenant
        if (currentUser.getTenant() != null && !currentUser.getTenant().getId().equals(user.getTenant().getId())) {
            return ResponseEntity.status(403).body("Você não pode editar usuários de outro condomínio.");
        }

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
            changes.append("Unidade mudou para " + data.unidade() + ". ");
        }
        if (data.bloco() != null && !data.bloco().equals(user.getBloco())) {
            user.setBloco(data.bloco());
            changes.append("Bloco mudou para " + data.bloco() + ". ");
        }

        if (data.role() != null && (currentUser.getRole() == Role.SINDICO || currentUser.getRole() == Role.ADM_CONDO)) {
            try {
                Role newRole = Role.valueOf(data.role());
                if (newRole != Role.ADMIN && newRole != user.getRole()) {
                    user.setRole(newRole);
                    changes.append("Cargo alterado para " + newRole + ". ");
                }
            } catch (Exception e) {} 
        }

        if (data.password() != null && !data.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(data.password()));
            changes.append("SENHA ALTERADA. ");
        }

        userRepository.save(user);

        if (!changes.isEmpty()) {
            // --- CORREÇÃO: Passando user.getTenant() como targetTenant ---
            // Se for Admin Votzz (sem tenant), o log vai para o tenant do usuário editado
            auditService.log(
                currentUser, 
                user.getTenant(), // targetTenant
                "EDITAR_USUARIO", 
                "Alterações em " + user.getNome() + ": " + changes.toString(), 
                "USUARIOS"
            );
        }

        return ResponseEntity.ok("Dados atualizados.");
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
    public ResponseEntity<?> promoteUser(@PathVariable UUID id, @RequestBody java.util.Map<String, String> payload, @AuthenticationPrincipal User currentUser) {
        User user = userRepository.findById(id).orElseThrow();
        String newRoleStr = payload.get("role");
        
        if("MANAGER".equals(newRoleStr) || "ADM_CONDO".equals(newRoleStr)) {
            user.setRole(Role.ADM_CONDO); 
            userRepository.save(user);
            
            // --- CORREÇÃO: Passando user.getTenant() como targetTenant ---
            auditService.log(
                currentUser, 
                user.getTenant(), // targetTenant
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