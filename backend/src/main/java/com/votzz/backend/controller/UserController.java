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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador responsável pelo gerenciamento de usuários.
 * ATUALIZADO: Lógica de Perfil Universal (Multi-Tenant com mesmo e-mail).
 */
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

    // =================================================================================
    // 1. CRIAR NOVO USUÁRIO (POST) - CORRIGIDO PARA PERFIL UNIVERSAL
    // =================================================================================
    @PostMapping
    @Transactional
    public ResponseEntity<?> createUser(@RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        // --- LOGS DE DEBUG ---
        System.out.println(">>> [POST] Criando usuário: " + data.nome());
        System.out.println(">>> E-mail: " + data.email());

        // 1. Validação de Segurança Básica: Quem cria deve ter um condomínio (exceto Admin Global, mas assumimos contexto local aqui)
        if (currentUser.getTenant() == null) {
            return ResponseEntity.status(403).body("Você precisa estar vinculado a um condomínio para criar usuários.");
        }

        // 2. Validação de E-mail (LÓGICA PERFIL UNIVERSAL)
        // Buscamos todas as contas com esse e-mail no sistema inteiro
        List<User> existingAccounts = userRepository.findByEmailIgnoreCase(data.email());

        // Verificamos se JÁ existe uma conta com esse e-mail NESTE condomínio específico
        boolean existsInCurrentTenant = existingAccounts.stream()
                .anyMatch(u -> u.getTenant() != null && u.getTenant().getId().equals(currentUser.getTenant().getId()));

        if (existsInCurrentTenant) {
            return ResponseEntity.badRequest().body("Erro: O e-mail informado já está cadastrado neste condomínio. Utilize a edição para adicionar unidades.");
        }

        // 3. Validação de CPF (Global por Pessoa)
        // Se informado, o CPF deve ser único para a PESSOA, não para a conta.
        // Se o e-mail for diferente, mas o CPF for igual, bloqueamos (uma pessoa = um CPF).
        if (data.cpf() != null && !data.cpf().isEmpty()) {
            boolean cpfUsedByAnotherPerson = userRepository.findAll().stream()
                    .anyMatch(u -> data.cpf().equals(u.getCpf()) && !u.getEmail().equalsIgnoreCase(data.email()));
            
            if (cpfUsedByAnotherPerson) {
                return ResponseEntity.badRequest().body("Erro: Este CPF já está vinculado a um e-mail diferente.");
            }
        }

        // 4. Montagem do Objeto Usuário
        User newUser = new User();
        newUser.setNome(data.nome());
        newUser.setEmail(data.email());
        newUser.setWhatsapp(data.whatsapp());
        
        // Salva a unidade principal
        newUser.setUnidade(data.unidade());
        newUser.setBloco(data.bloco());
        
        // Salva a lista de unidades
        if (data.unidadesList() != null && !data.unidadesList().isEmpty()) {
            newUser.setUnidadesList(data.unidadesList()); 
        } else {
            if (data.unidade() != null && !data.unidade().isEmpty()) {
                String unidadeFormatada = data.unidade() + (data.bloco() != null ? " - " + data.bloco() : "");
                List<String> listaInicial = new ArrayList<>();
                listaInicial.add(unidadeFormatada);
                newUser.setUnidadesList(listaInicial);
            }
        }

        // Vincula ao condomínio atual de quem está criando
        newUser.setTenant(currentUser.getTenant());

        // 5. Definição de Cargo (Role)
        if ((currentUser.getRole() == Role.SINDICO || currentUser.getRole() == Role.ADM_CONDO) && data.role() != null) {
            try {
                Role requestedRole = Role.valueOf(data.role());
                // Proteção: Não permite criar um ADMIN (Superuser) por esta rota local
                if (requestedRole == Role.ADMIN) {
                    newUser.setRole(Role.MORADOR);
                } else {
                    newUser.setRole(requestedRole);
                }
            } catch (Exception e) {
                newUser.setRole(Role.MORADOR);
            }
        } else {
            newUser.setRole(Role.MORADOR);
        }

        // 6. Definição de Senha e Sincronização (A MÁGICA DO PERFIL UNIVERSAL)
        if (!existingAccounts.isEmpty()) {
            // Se o usuário já existe em outro condomínio (ex: é Síndico no condomínio A e agora será Morador no B),
            // COPIAMOS os dados sensíveis para manter o Login Universal.
            User referenceProfile = existingAccounts.get(0);
            
            System.out.println(">>> Sincronizando com perfil existente (Universal Profile) ID: " + referenceProfile.getId());
            
            newUser.setPassword(referenceProfile.getPassword()); // Usa a mesma senha criptografada
            newUser.setCpf(referenceProfile.getCpf());           // Usa o mesmo CPF
            newUser.setSecret2fa(referenceProfile.getSecret2fa()); // Mantém o mesmo 2FA
            newUser.setIs2faEnabled(referenceProfile.getIs2faEnabled());
            
        } else {
            // É um usuário totalmente novo no sistema Votzz
            newUser.setCpf(data.cpf()); 
            
            if (data.password() != null && !data.password().isBlank()) {
                newUser.setPassword(passwordEncoder.encode(data.password()));
            } else {
                newUser.setPassword(passwordEncoder.encode("votzz123")); // Senha padrão
            }
        }

        // 7. Persistência
        User savedUser = userRepository.save(newUser);
        
        // 8. Auditoria
        auditService.log(
                currentUser,
                currentUser.getTenant(),
                "CRIAR_USUARIO",
                "Criou o usuário " + newUser.getNome() + " (Unidade: " + newUser.getUnidade() + ")",
                "USUARIOS"
        );

        return ResponseEntity.ok("Usuário criado com sucesso!");
    }

    // =================================================================================
    // 2. ATUALIZAR USUÁRIO (PATCH)
    // =================================================================================
    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        return processUpdate(id, data, currentUser);
    }

    // =================================================================================
    // 3. ATUALIZAR USUÁRIO (PUT)
    // =================================================================================
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateUserPut(@PathVariable UUID id, @RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        return processUpdate(id, data, currentUser);
    }

    private ResponseEntity<?> processUpdate(UUID id, UserDTO data, User currentUser) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        StringBuilder changes = new StringBuilder();

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(403).body("Ninguém pode alterar o Super Admin do sistema.");
        }

        // VERIFICAÇÃO DE PERMISSÕES
        boolean isEditingSelf = currentUser.getId().equals(user.getId()) ||
                (currentUser.getEmail().equalsIgnoreCase(user.getEmail()) &&
                        (currentUser.getTenant() == null || user.getTenant() == null ||
                                currentUser.getTenant().getId().equals(user.getTenant().getId())));

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

        // --- ATUALIZAÇÃO DOS CAMPOS ---

        // 1. E-mail (Mudança de e-mail afeta o login universal)
        if (data.email() != null && !data.email().isEmpty() && !data.email().equals(user.getEmail())) {
             // Verifica se o novo e-mail já está em uso por OUTRA pessoa
             List<User> othersWithEmail = userRepository.findByEmailIgnoreCase(data.email());
             boolean emailTakenBySomeoneElse = othersWithEmail.stream()
                .anyMatch(u -> !u.getId().equals(user.getId())); // Se existe registro com ID diferente

             // Se for apenas outro perfil da mesma pessoa (universal), tudo bem. Mas aqui simplificamos para evitar conflito de merge.
             // Para simplificar: Não permitimos mudar para um e-mail que já existe em outro usuário diferente.
             // Mas permitimos mudar para um e-mail novo.
             if (emailTakenBySomeoneElse) {
                  // Aqui poderíamos adicionar lógica complexa de merge, mas por segurança bloqueamos duplicidade na edição
                  return ResponseEntity.badRequest().body("O novo e-mail já está em uso.");
             }
             
            user.setEmail(data.email());
            changes.append("Email alterado. ");
        }

        // 2. CPF (Sincronizado)
        if (data.cpf() != null && !data.cpf().isEmpty() && !data.cpf().equals(user.getCpf())) {
            // Verifica duplicidade global
            boolean cpfExists = userRepository.findAll().stream()
                    .anyMatch(u -> data.cpf().equals(u.getCpf()) && !u.getEmail().equalsIgnoreCase(user.getEmail()));
            
            if (cpfExists) {
                return ResponseEntity.badRequest().body("O novo CPF já está cadastrado em outra conta.");
            }
            user.setCpf(data.cpf());
            changes.append("CPF alterado. ");
            
            // Sincroniza CPF em todos os perfis desse email (Universal Profile Sync)
            List<User> allMyProfiles = userRepository.findByEmailIgnoreCase(user.getEmail());
            for (User p : allMyProfiles) {
                p.setCpf(data.cpf());
                userRepository.save(p);
            }
        }

        // 3. WhatsApp
        if (data.whatsapp() != null && !data.whatsapp().equals(user.getWhatsapp())) {
            user.setWhatsapp(data.whatsapp());
            changes.append("WhatsApp alterado. ");
        }

        // 4. Unidade e Bloco
        if (data.unidade() != null && !data.unidade().equals(user.getUnidade())) {
            user.setUnidade(data.unidade());
            changes.append("Unidade Principal mudou. ");
        }
        if (data.bloco() != null && !data.bloco().equals(user.getBloco())) {
            user.setBloco(data.bloco());
            changes.append("Bloco mudou. ");
        }

        // 5. Lista de Unidades
        if (data.unidadesList() != null) {
            user.setUnidadesList(data.unidadesList());
            changes.append("Lista de Unidades atualizada. ");
        }

        // 6. Role
        if (data.role() != null && (currentUser.getRole() == Role.SINDICO || currentUser.getRole() == Role.ADM_CONDO)) {
            try {
                Role newRole = Role.valueOf(data.role());
                if (newRole != Role.ADMIN && newRole != user.getRole()) {
                    user.setRole(newRole);
                    changes.append("Cargo alterado para " + newRole + ". ");
                }
            } catch (Exception e) {}
        }

        // 7. SENHA (Sincronizada Globalmente pelo E-mail)
        if (data.password() != null && !data.password().isBlank()) {
            String encodedPassword = passwordEncoder.encode(data.password());
            
            // Busca TODOS os perfis com este mesmo e-mail para atualizar a senha em tudo
            List<User> allMyProfiles = userRepository.findByEmailIgnoreCase(user.getEmail());
            
            if (allMyProfiles.isEmpty()) {
                user.setPassword(encodedPassword);
                userRepository.save(user);
            } else {
                for (User p : allMyProfiles) {
                    p.setPassword(encodedPassword);
                    userRepository.save(p);
                }
            }
            changes.append("SENHA ATUALIZADA (Sincronizada em todos os perfis). ");
        } else {
            // Se não mudou senha, salva apenas as outras alterações
            userRepository.save(user);
        }

        if (changes.length() > 0) {
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

    // =================================================================================
    // 4. LISTAR TODOS OS USUÁRIOS
    // =================================================================================
    @GetMapping
    public ResponseEntity<List<User>> listAll(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getTenant() == null) {
            if (currentUser.getRole() == Role.ADMIN) {
                // Admin Global vê tudo
                return ResponseEntity.ok(userRepository.findAll());
            }
            return ResponseEntity.ok(List.of());
        }

        // Lista apenas usuários DESTE condomínio
        List<User> users = userRepository.findByTenantId(currentUser.getTenant().getId());
        return ResponseEntity.ok(users);
    }

    // =================================================================================
    // 5. PROMOVER USUÁRIO
    // =================================================================================
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

    // DTO
    public record UserDTO(
        String nome, 
        String email, 
        String cpf, 
        String whatsapp, 
        String unidade, 
        String bloco, 
        List<String> unidadesList, 
        String role, 
        String password
    ) {}
}