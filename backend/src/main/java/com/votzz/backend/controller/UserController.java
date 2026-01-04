package com.votzz.backend.controller;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    // --- CRIAR NOVO USUÁRIO (Chamado pelo Modal "Novo Usuário") ---
    @PostMapping
    @Transactional
    public ResponseEntity<?> createUser(@RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        if (currentUser.getTenant() == null) {
            return ResponseEntity.status(403).body("Você precisa estar vinculado a um condomínio.");
        }

        // Validação de e-mail e CPF
        if (userRepository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Email já cadastrado.");
        }
        if (data.cpf() != null && !data.cpf().isEmpty()) {
            // Lógica simples: verifica se CPF já existe no banco geral
            // (Para 'Afiliado aparecer se tiver conta de morador', o ideal é permitir CPF duplicado se os tenants forem diferentes, 
            // mas aqui seguimos a regra básica de unicidade ou vínculo manual)
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
        
        // Define Cargo (Se o criador for Sindico/Admin, aceita o cargo enviado, senão default MORADOR)
        if ((currentUser.getRole() == Role.SINDICO || currentUser.getRole() == Role.ADM_CONDO) && data.role() != null) {
            try {
                newUser.setRole(Role.valueOf(data.role()));
            } catch (Exception e) { newUser.setRole(Role.MORADOR); }
        } else {
            newUser.setRole(Role.MORADOR);
        }

        // Senha inicial
        if (data.password() != null && !data.password().isBlank()) {
            newUser.setPassword(passwordEncoder.encode(data.password()));
        } else {
            newUser.setPassword(passwordEncoder.encode("votzz123"));
        }

        userRepository.save(newUser);
        return ResponseEntity.ok("Usuário criado com sucesso!");
    }

    // --- ATUALIZAR USUÁRIO (Restrições aplicadas) ---
    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        return processUpdate(id, data, currentUser);
    }

    @PutMapping("/{id}") // Frontend pode mandar PUT também
    @Transactional
    public ResponseEntity<?> updateUserPut(@PathVariable UUID id, @RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        return processUpdate(id, data, currentUser);
    }

    private ResponseEntity<?> processUpdate(UUID id, UserDTO data, User currentUser) {
        var userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOptional.get();

        // 1. PROTEÇÃO DO ADMIN DA VOTZZ
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(403).body("Ninguém pode alterar o Super Admin da Votzz.");
        }

        // 2. Validação de Permissão (Só pode editar gente do mesmo condomínio)
        if (currentUser.getTenant() != null && !currentUser.getTenant().getId().equals(user.getTenant().getId())) {
            return ResponseEntity.status(403).body("Você não pode editar usuários de outro condomínio.");
        }

        // 3. Atualizações Permitidas (NOME E CPF SÃO IGNORADOS AQUI PARA EDIÇÃO)
        // O prompt diz: "quero poder mudar tudo da pessoa, menos cpf e nome"
        
        if (data.email() != null && !data.email().isEmpty() && !data.email().equals(user.getEmail())) {
            var emailExists = userRepository.findByEmail(data.email());
            if (emailExists.isPresent()) return ResponseEntity.badRequest().body("E-mail em uso.");
            user.setEmail(data.email());
        }

        if (data.whatsapp() != null) user.setWhatsapp(data.whatsapp());
        if (data.unidade() != null) user.setUnidade(data.unidade());
        if (data.bloco() != null) user.setBloco(data.bloco());

        // Atualização de Cargo (Apenas SINDICO/ADM pode mudar cargos, mas não para virar ADMIN VOTZZ)
        if (data.role() != null && (currentUser.getRole() == Role.SINDICO || currentUser.getRole() == Role.ADM_CONDO)) {
            try {
                Role newRole = Role.valueOf(data.role());
                if (newRole != Role.ADMIN) { // Segurança extra
                    user.setRole(newRole);
                }
            } catch (Exception e) {} // Ignora roles inválidas
        }

        if (data.password() != null && !data.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(data.password()));
        }

        userRepository.save(user);
        return ResponseEntity.ok("Dados atualizados (Nome e CPF preservados).");
    }

    // --- LISTAR APENAS DO CONDOMÍNIO ---
    @GetMapping
    public ResponseEntity<List<User>> listAll(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getTenant() == null) {
            // Se for o Super Admin Votzz sem tenant, talvez liste tudo ou vazio.
            // Aqui assumimos lista vazia ou todos se for ADMIN global debugging.
            if (currentUser.getRole() == Role.ADMIN) return ResponseEntity.ok(userRepository.findAll());
            return ResponseEntity.ok(List.of());
        }
        
        // Retorna apenas usuários deste Tenant
        // A query derived seria findAllByTenant(currentUser.getTenant())
        // Como o Repository padrão talvez não tenha, fazemos stream filter se a base for pequena,
        // mas o ideal é ter o método no Repository. Vou usar stream aqui para garantir compatibilidade com seu repo atual.
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getTenant() != null && u.getTenant().getId().equals(currentUser.getTenant().getId()))
                .toList();

        return ResponseEntity.ok(users);
    }
    
    // Endpoint auxiliar para promover (usado no Dashboard)
    @PatchMapping("/{id}/role")
    public ResponseEntity<?> promoteUser(@PathVariable UUID id, @RequestBody java.util.Map<String, String> payload) {
        User user = userRepository.findById(id).orElseThrow();
        // Logica simplificada, validando role MANAGER/ADM_CONDO
        String newRole = payload.get("role");
        if("MANAGER".equals(newRole) || "ADM_CONDO".equals(newRole)) {
            user.setRole(Role.ADM_CONDO); 
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    // DTO Unificado
    public record UserDTO(String nome, String email, String cpf, String whatsapp, String unidade, String bloco, String role, String password) {}
}