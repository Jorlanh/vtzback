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
 * Controlador responsável pelo gerenciamento de usuários (Moradores, Síndicos, Staff).
 * Inclui criação, edição, listagem e promoção de cargos.
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
    // 1. CRIAR NOVO USUÁRIO (POST)
    // =================================================================================
    @PostMapping
    @Transactional
    public ResponseEntity<?> createUser(@RequestBody UserDTO data, @AuthenticationPrincipal User currentUser) {
        // --- LOGS DE DEBUG PARA VERIFICAR DADOS CHEGANDO ---
        System.out.println(">>> [POST] Criando usuário: " + data.nome());
        System.out.println(">>> CPF: " + data.cpf());
        System.out.println(">>> Lista de Unidades recebida: " + data.unidadesList());

        // 1. Validação de Segurança Básica: Quem cria deve ter um condomínio
        if (currentUser.getTenant() == null) {
            return ResponseEntity.status(403).body("Você precisa estar vinculado a um condomínio para criar usuários.");
        }

        // 2. Validação de E-mail Único
        if (userRepository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Erro: O e-mail informado já está cadastrado no sistema.");
        }

        // 3. Validação de CPF Único (Se informado)
        if (data.cpf() != null && !data.cpf().isEmpty()) {
            // Verifica se o CPF existe em qualquer usuário do sistema
            boolean cpfExists = userRepository.findAll().stream()
                    .anyMatch(u -> data.cpf().equals(u.getCpf()));
            
            if (cpfExists) {
                return ResponseEntity.badRequest().body("Erro: Este CPF já está cadastrado para outro usuário.");
            }
        }

        // 4. Montagem do Objeto Usuário
        User newUser = new User();
        newUser.setNome(data.nome());
        newUser.setEmail(data.email());
        newUser.setCpf(data.cpf());         
        newUser.setWhatsapp(data.whatsapp());
        
        // Salva a unidade principal (campos legados para compatibilidade)
        newUser.setUnidade(data.unidade());
        newUser.setBloco(data.bloco());
        
        // --- SALVAMENTO DA LISTA DE UNIDADES ---
        // Aqui corrigimos o problema visual: salvamos a lista completa no banco.
        if (data.unidadesList() != null && !data.unidadesList().isEmpty()) {
            newUser.setUnidadesList(data.unidadesList()); 
        } else {
            // Se a lista vier vazia, mas tiver unidade única, criamos uma lista com ela
            if (data.unidade() != null && !data.unidade().isEmpty()) {
                String unidadeFormatada = data.unidade() + (data.bloco() != null ? " - " + data.bloco() : "");
                List<String> listaInicial = new ArrayList<>();
                listaInicial.add(unidadeFormatada);
                newUser.setUnidadesList(listaInicial);
            }
        }

        newUser.setTenant(currentUser.getTenant());

        // 5. Definição de Cargo (Role)
        // Apenas Síndicos e Admins podem criar cargos elevados. Padrão é MORADOR.
        if ((currentUser.getRole() == Role.SINDICO || currentUser.getRole() == Role.ADM_CONDO) && data.role() != null) {
            try {
                Role requestedRole = Role.valueOf(data.role());
                // Proteção: Não permite criar um ADMIN (Superuser) por esta rota
                if (requestedRole == Role.ADMIN) {
                    newUser.setRole(Role.MORADOR);
                } else {
                    newUser.setRole(requestedRole);
                }
            } catch (Exception e) {
                // Se a role for inválida, define como morador
                newUser.setRole(Role.MORADOR);
            }
        } else {
            newUser.setRole(Role.MORADOR);
        }

        // 6. Definição de Senha
        if (data.password() != null && !data.password().isBlank()) {
            newUser.setPassword(passwordEncoder.encode(data.password()));
        } else {
            // Senha padrão se não informada
            newUser.setPassword(passwordEncoder.encode("votzz123"));
        }

        // 7. Persistência no Banco de Dados
        User savedUser = userRepository.save(newUser);
        
        System.out.println(">>> Usuário salvo com ID: " + savedUser.getId());

        // 8. Auditoria
        auditService.log(
                currentUser,
                currentUser.getTenant(),
                "CRIAR_USUARIO",
                "Criou o usuário " + newUser.getNome() + " (Unidade Principal: " + newUser.getUnidade() + ")",
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

    /**
     * Lógica central de atualização de usuário.
     * Contém regras de permissão, atualização de campos e replicação de senha para multi-contas.
     */
    private ResponseEntity<?> processUpdate(UUID id, UserDTO data, User currentUser) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        StringBuilder changes = new StringBuilder();

        // Regra de Proteção Global: Ninguém edita o Super Admin via API comum
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(403).body("Ninguém pode alterar o Super Admin do sistema.");
        }

        // -------------------------------------------------------------------------
        // VERIFICAÇÃO DE PERMISSÕES
        // -------------------------------------------------------------------------
        System.out.println(">>> [UPDATE] Tentativa de edição no usuário ID: " + user.getId());
        
        // Verifica se é auto-edição (pelo ID ou e-mail correspondente)
        boolean isEditingSelf = currentUser.getId().equals(user.getId()) ||
                (currentUser.getEmail().equalsIgnoreCase(user.getEmail()) &&
                        (currentUser.getTenant() == null || user.getTenant() == null ||
                                currentUser.getTenant().getId().equals(user.getTenant().getId())));

        if (!isEditingSelf) {
            // Se não é ele mesmo, precisa ser Gestor do MESMO condomínio
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

        // -------------------------------------------------------------------------
        // ATUALIZAÇÃO DOS CAMPOS (Se presentes no JSON)
        // -------------------------------------------------------------------------

        // 1. E-mail
        if (data.email() != null && !data.email().isEmpty() && !data.email().equals(user.getEmail())) {
            // Verifica se o novo email já existe
            if (userRepository.findByEmail(data.email()).isPresent()) {
                return ResponseEntity.badRequest().body("O novo e-mail já está em uso por outro usuário.");
            }
            user.setEmail(data.email());
            changes.append("Email alterado. ");
        }

        // 2. CPF (Com verificação de duplicidade)
        if (data.cpf() != null && !data.cpf().isEmpty() && !data.cpf().equals(user.getCpf())) {
             boolean cpfExists = userRepository.findAll().stream()
                    .anyMatch(u -> data.cpf().equals(u.getCpf()) && !u.getId().equals(user.getId()));
            
            if (cpfExists) {
                return ResponseEntity.badRequest().body("O novo CPF já está cadastrado.");
            }
            user.setCpf(data.cpf());
            changes.append("CPF alterado. ");
        }

        // 3. WhatsApp
        if (data.whatsapp() != null && !data.whatsapp().equals(user.getWhatsapp())) {
            user.setWhatsapp(data.whatsapp());
            changes.append("WhatsApp alterado. ");
        }

        // 4. Unidade e Bloco (Principal/Legado)
        if (data.unidade() != null && !data.unidade().equals(user.getUnidade())) {
            user.setUnidade(data.unidade());
            changes.append("Unidade Principal mudou. ");
        }
        if (data.bloco() != null && !data.bloco().equals(user.getBloco())) {
            user.setBloco(data.bloco());
            changes.append("Bloco mudou. ");
        }

        // 5. LISTA DE UNIDADES (Atualização)
        if (data.unidadesList() != null) {
            System.out.println(">>> Atualizando lista de unidades para: " + data.unidadesList());
            user.setUnidadesList(data.unidadesList());
            changes.append("Lista de Unidades atualizada. ");
        }

        // 6. Role (Cargo) - Apenas Gestores podem alterar Role de terceiros
        if (data.role() != null && (currentUser.getRole() == Role.SINDICO || currentUser.getRole() == Role.ADM_CONDO)) {
            try {
                Role newRole = Role.valueOf(data.role());
                if (newRole != Role.ADMIN && newRole != user.getRole()) {
                    user.setRole(newRole);
                    changes.append("Cargo alterado para " + newRole + ". ");
                }
            } catch (Exception e) {
                // Role inválida ignorada
            }
        }

        // -------------------------------------------------------------------------
        // ATUALIZAÇÃO DE SENHA (Com replicação para multi-unidades via CPF)
        // -------------------------------------------------------------------------
        if (data.password() != null && !data.password().isBlank()) {
            String encodedPassword = passwordEncoder.encode(data.password());

            // Se o usuário tem CPF cadastrado, tenta atualizar a senha de TODAS as unidades dele no mesmo condomínio
            if (user.getCpf() != null && !user.getCpf().isBlank() && user.getTenant() != null) {
                List<User> todasAsUnidades = userRepository.findAll().stream()
                        .filter(u -> user.getCpf().equals(u.getCpf()) &&
                                u.getTenant() != null &&
                                u.getTenant().getId().equals(user.getTenant().getId()))
                        .collect(Collectors.toList());

                if (todasAsUnidades.isEmpty()) {
                    // Fallback (apenas ele mesmo)
                    user.setPassword(encodedPassword);
                } else {
                    for (User unidade : todasAsUnidades) {
                        unidade.setPassword(encodedPassword);
                        userRepository.save(unidade);
                    }
                }
                changes.append("SENHA ATUALIZADA (Sincronizada em todas as contas deste CPF). ");
            } else {
                // Atualização simples se não tiver CPF
                user.setPassword(encodedPassword);
                changes.append("SENHA ALTERADA. ");
            }
        }

        // Salva as alterações
        userRepository.save(user);

        // Log de Auditoria
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
        // Se usuário não tem tenant (ex: erro de cadastro ou admin global)
        if (currentUser.getTenant() == null) {
            if (currentUser.getRole() == Role.ADMIN) {
                // Admin global vê tudo
                return ResponseEntity.ok(userRepository.findAll());
            }
            return ResponseEntity.ok(List.of());
        }

        // Filtra apenas usuários do mesmo condomínio (Tenant Isolation)
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getTenant() != null && u.getTenant().getId().equals(currentUser.getTenant().getId()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(users);
    }

    // =================================================================================
    // 5. PROMOVER USUÁRIO (Endpoint Rápido)
    // =================================================================================
    @PatchMapping("/{id}/role")
    @Transactional
    public ResponseEntity<?> promoteUser(@PathVariable UUID id, @RequestBody java.util.Map<String, String> payload, @AuthenticationPrincipal User currentUser) {
        User user = userRepository.findById(id).orElseThrow();
        String newRoleStr = payload.get("role");

        // Permite promover apenas para níveis intermediários
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

    // =================================================================================
    // DTO (Data Transfer Object)
    // =================================================================================
    // Importante: O campo 'unidadesList' deve corresponder ao JSON enviado pelo Frontend
    public record UserDTO(
        String nome, 
        String email, 
        String cpf, 
        String whatsapp, 
        String unidade, 
        String bloco, 
        List<String> unidadesList, // <--- CAMPO ESSENCIAL PARA O ARRAY DE UNIDADES
        String role, 
        String password
    ) {}
}