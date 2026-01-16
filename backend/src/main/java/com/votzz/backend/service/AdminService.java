package com.votzz.backend.service;

import com.votzz.backend.controller.AdminController.ManualTenantDTO;
import com.votzz.backend.controller.AdminController.UpdateTenantDTO;
import com.votzz.backend.controller.AdminController.CreateUserRequest;
import com.votzz.backend.controller.AdminController.UpdateUserRequest;
import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.dto.AdminDashboardStats;
import com.votzz.backend.dto.UserDTO;
import com.votzz.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate; // Importante para dataExpiracaoPlano
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final CouponRepository couponRepository;
    private final PlanoRepository planoRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${votzz.admin.email}")
    private String masterEmail;

    private static final String MASTER_ADMIN_ID = "10000000-0000-0000-0000-000000000000";

    public List<AuditLog> listAuditLogs() {
        return auditLogRepository.findAll().stream()
                .filter(log -> {
                    if ("ADMIN_PANEL".equals(log.getResourceType())) return true;
                    try {
                        User actor = userRepository.findById(UUID.fromString(log.getUserId())).orElse(null);
                        return actor != null && actor.getRole() == Role.ADMIN;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .sorted(Comparator.comparing(AuditLog::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(100)
                .collect(Collectors.toList());
    }

    public AdminDashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long onlineUsers = userRepository.countOnlineUsers(LocalDateTime.now().minusMinutes(5));
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByAtivoTrue();

        List<Tenant> activeTenantsList = tenantRepository.findByAtivoTrue();
        BigDecimal mrr = BigDecimal.ZERO;

        for (Tenant t : activeTenantsList) {
            if (t.getPlano() != null && t.getPlano().getPrecoBase() != null) {
                BigDecimal preco = t.getPlano().getPrecoBase();
                if (t.getPlano().getCiclo() == Plano.Ciclo.ANUAL) {
                    mrr = mrr.add(preco.divide(new BigDecimal("12"), 2, RoundingMode.HALF_EVEN));
                } else {
                    mrr = mrr.add(preco);
                }
            }
        }
        return new AdminDashboardStats(totalUsers, onlineUsers, totalTenants, activeTenants, mrr);
    }

    public List<UserDTO> listAllAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<Tenant> listAllTenants() {
        return tenantRepository.findAll();
    }

    public List<Coupon> listAllCoupons() {
        return couponRepository.findAll();
    }

    public Map<String, Object> listOrganizedUsers() {
        List<User> allUsers = userRepository.findAll();
        List<Tenant> allTenants = tenantRepository.findAll();

        List<UserDTO> afiliados = allUsers.stream()
                .filter(u -> u.getRole() == Role.AFILIADO)
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        Map<String, List<UserDTO>> pastas = new HashMap<>();
        for (Tenant t : allTenants) {
            pastas.put(t.getNome(), new ArrayList<>());
        }
        pastas.put("Sem Condomínio", new ArrayList<>());

        for (User u : allUsers) {
            if (u.getRole() == Role.ADMIN || u.getRole() == Role.AFILIADO) continue;

            UserDTO dto = mapToDTO(u);
            if (u.getTenants().isEmpty()) {
                pastas.get("Sem Condomínio").add(dto);
            } else {
                for (Tenant t : u.getTenants()) {
                    if (pastas.containsKey(t.getNome())) {
                        pastas.get(t.getNome()).add(dto);
                    }
                }
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("afiliados", afiliados);
        resp.put("pastas", pastas);
        return resp;
    }

    @Transactional
    public void createUserLinked(CreateUserRequest dto) {
        User existingUser = userRepository.findByEmail(dto.email()).orElse(null);
        if (existingUser != null) {
             throw new RuntimeException("E-mail já está em uso. Para vincular um usuário existente a este condomínio, use a função de convite (Futura implementação).");
        }

        if (dto.cpf() != null && !dto.cpf().isEmpty()) {
            boolean cpfExists = userRepository.findAll().stream()
                    .anyMatch(u -> dto.cpf().equals(u.getCpf()));
            if (cpfExists) throw new RuntimeException("CPF já cadastrado no sistema.");
        }

        Tenant tenant = tenantRepository.findById(UUID.fromString(dto.tenantId()))
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado."));

        User user = new User();
        user.setNome(dto.nome());
        user.setEmail(dto.email());
        user.setCpf(dto.cpf());
        user.setWhatsapp(dto.whatsapp());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(Role.valueOf(dto.role()));
        
        user.setTenants(new ArrayList<>());
        user.getTenants().add(tenant);
        
        user.setUnidade(dto.unidade());
        user.setBloco(dto.bloco());

        userRepository.save(user);
        logAction("CRIAR_USUARIO", "Criou usuário " + user.getNome() + " (" + user.getRole() + ") no condomínio " + tenant.getNome());
    }

    @Transactional
    public void createTenantManual(ManualTenantDTO dto) {
        Plano plano = null;
        if (dto.plano() != null && !dto.plano().isBlank()) {
            plano = planoRepository.findByNomeIgnoreCase(dto.plano())
                    .orElseThrow(() -> new RuntimeException("Plano não encontrado no sistema: " + dto.plano()));
        } else {
            plano = planoRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("ERRO: Nenhum Plano cadastrado no sistema."));
        }

        Tenant tenant = new Tenant();
        tenant.setNome(dto.condoName());
        tenant.setCnpj(dto.cnpj());
        tenant.setUnidadesTotal(dto.qtyUnits());
        tenant.setSecretKeyword(dto.secretKeyword());
        tenant.setPlano(plano);
        tenant.setAtivo(true);
        tenant.setStatusAssinatura("PAID");
        tenant.setCep(dto.cep());
        tenant.setLogradouro(dto.logradouro());
        tenant.setNumero(dto.numero());
        tenant.setBairro(dto.bairro());
        tenant.setCidade(dto.cidade());
        tenant.setEstado(dto.estado());
        tenant.setPontoReferencia(dto.pontoReferencia());
        
        // CORREÇÃO: Uso de LocalDate em vez de LocalDateTime
        if(dto.dataExpiracaoPlano() != null) {
            tenant.setDataExpiracaoPlano(dto.dataExpiracaoPlano());
        } else {
            // Padrão 1 ano se não informado
            tenant.setDataExpiracaoPlano(LocalDate.now().plusYears(1)); 
        }

        tenant = tenantRepository.save(tenant);

        // --- LÓGICA MULTI-TENANT PARA SÍNDICO ---
        User syndic = userRepository.findByEmail(dto.emailSyndic()).orElse(null);

        if (syndic != null) {
            System.out.println(">>> Usuário Síndico existente encontrado. Vinculando...");
            
            if (syndic.getCpf() != null && !syndic.getCpf().equals(dto.cpfSyndic())) {
                throw new RuntimeException("O e-mail informado já pertence a um usuário com outro CPF.");
            }

            if (!syndic.getTenants().contains(tenant)) {
                syndic.getTenants().add(tenant);
            }
            
            if (syndic.getRole() == Role.MORADOR) {
                syndic.setRole(Role.SINDICO);
            }
            
            // Opcional: Atualiza o tenant 'default' para o mais recente
            syndic.setTenant(tenant); 

        } else {
            System.out.println(">>> Criando novo usuário Síndico...");
            syndic = new User();
            syndic.setNome(dto.nameSyndic());
            syndic.setEmail(dto.emailSyndic());
            syndic.setCpf(dto.cpfSyndic());
            syndic.setWhatsapp(dto.phoneSyndic());
            syndic.setPassword(passwordEncoder.encode(dto.passwordSyndic()));
            syndic.setRole(Role.SINDICO);
            
            syndic.setTenants(new ArrayList<>());
            syndic.getTenants().add(tenant);
            syndic.setTenant(tenant); // Define padrão
            
            syndic.setUnidade("ADM");
            syndic.setBloco("ADM");
        }

        userRepository.save(syndic);

        logAction("CRIAR_CONDOMINIO", "Criou condomínio " + tenant.getNome() + " vinculado ao síndico " + syndic.getNome());
    }

    @Transactional
    public void updateTenant(UUID tenantId, UpdateTenantDTO dto) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));
        StringBuilder detailsLog = new StringBuilder("Alterou Condomínio " + tenant.getNome() + ": ");
        boolean changed = false;

        if (hasChanged(tenant.getNome(), dto.nome())) {
            tenant.setNome(dto.nome());
            changed = true;
        }
        if (hasChanged(tenant.getCnpj(), dto.cnpj())) {
            tenant.setCnpj(dto.cnpj());
            changed = true;
        }
        if (dto.unidadesTotal() != null && !dto.unidadesTotal().equals(tenant.getUnidadesTotal())) {
            tenant.setUnidadesTotal(dto.unidadesTotal());
            changed = true;
        }

        if (dto.ativo() != null && dto.ativo() != tenant.isAtivo()) {
            tenant.setAtivo(dto.ativo());
            detailsLog.append(dto.ativo() ? "[GOD MODE: ATIVOU] " : "[GOD MODE: BLOQUEOU] ");
            changed = true;
        }

        if (dto.plano() != null) {
            planoRepository.findByNomeIgnoreCase(dto.plano()).ifPresent(p -> {
                if (!p.equals(tenant.getPlano())) {
                    tenant.setPlano(p);
                    detailsLog.append("Plano -> ").append(p.getNome()).append(". ");
                }
            });
            changed = true;
        }

        // CORREÇÃO: Uso de LocalDate
        if (dto.dataExpiracaoPlano() != null) {
            tenant.setDataExpiracaoPlano(dto.dataExpiracaoPlano());
            detailsLog.append("Validade Manual -> ").append(dto.dataExpiracaoPlano()).append(". ");
            changed = true;
        }

        if (hasChanged(tenant.getSecretKeyword(), dto.secretKeyword())) tenant.setSecretKeyword(dto.secretKeyword());
        if (hasChanged(tenant.getCep(), dto.cep())) tenant.setCep(dto.cep());
        if (hasChanged(tenant.getLogradouro(), dto.logradouro())) tenant.setLogradouro(dto.logradouro());
        if (hasChanged(tenant.getNumero(), dto.numero())) tenant.setNumero(dto.numero());
        if (hasChanged(tenant.getBairro(), dto.bairro())) tenant.setBairro(dto.bairro());
        if (hasChanged(tenant.getCidade(), dto.cidade())) tenant.setCidade(dto.cidade());
        if (hasChanged(tenant.getEstado(), dto.estado())) tenant.setEstado(dto.estado());
        if (hasChanged(tenant.getPontoReferencia(), dto.pontoReferencia()))
            tenant.setPontoReferencia(dto.pontoReferencia());

        tenantRepository.save(tenant);
        if (changed) logAction("EDITAR_CONDOMINIO", detailsLog.toString());
    }

    @Transactional
    public void softDeleteTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));
        tenant.setAtivo(false); 
        tenantRepository.save(tenant);
        logAction("EXCLUIR_CONDOMINIO", "Soft Delete (Desativou) condomínio: " + tenant.getNome());
    }

    @Transactional
    public void createCoupon(String code, BigDecimal discount, Integer quantity) {
        Coupon coupon = new Coupon();
        coupon.setCode(code.toUpperCase());
        coupon.setDiscountPercent(discount);
        coupon.setQuantity(quantity);
        coupon.setActive(true);
        couponRepository.save(coupon);
        logAction("CRIAR_CUPOM", "Criou cupom " + code);
    }

    @Transactional
    public void deleteCoupon(UUID id) {
        couponRepository.deleteById(id);
        logAction("DELETAR_CUPOM", "Removeu cupom ID: " + id);
    }

    @Transactional
    public void createNewAdmin(String nome, String email, String cpf, String whatsapp, String password) {
        if (userRepository.findByEmail(email).isPresent()) throw new RuntimeException("E-mail já existe.");
        User admin = new User();
        admin.setNome(nome);
        admin.setEmail(email);
        admin.setCpf(cpf);
        admin.setWhatsapp(whatsapp);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        logAction("CRIAR_ADMIN", "Criou novo administrador: " + nome);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User target = userRepository.findById(userId).orElseThrow();
        User currentUser = getCurrentUser();

        if (target.getId().toString().equals(MASTER_ADMIN_ID))
            throw new RuntimeException("O Super Admin Mestre não pode ser removido.");
        if (target.getRole() == Role.ADMIN && !currentUser.getId().toString().equals(MASTER_ADMIN_ID)) {
            throw new RuntimeException("Apenas o Super Admin pode remover outros administradores.");
        }

        userRepository.delete(target);
        logAction("DELETAR_USUARIO", "Removeu usuário " + target.getNome() + " (" + target.getEmail() + ")");
    }

    @Transactional
    public void adminUpdateUser(UUID userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        User currentUser = getCurrentUser();

        if (user.getId().toString().equals(MASTER_ADMIN_ID) && !currentUser.getId().toString().equals(MASTER_ADMIN_ID)) {
            throw new RuntimeException("Você não tem permissão para editar o Super Admin.");
        }

        List<String> changes = new ArrayList<>();

        if (req.email() != null && !req.email().isBlank() && !req.email().equals(user.getEmail())) {
            if (userRepository.findByEmail(req.email()).isPresent())
                throw new RuntimeException("Este e-mail já está em uso.");
            changes.add("Email: " + user.getEmail() + " -> " + req.email());
            user.setEmail(req.email());
        }

        if (hasChanged(user.getNome(), req.nome())) {
            changes.add("Nome: " + user.getNome() + " -> " + req.nome());
            user.setNome(req.nome());
        }
        if (hasChanged(user.getCpf(), req.cpf())) {
            changes.add("CPF alterado");
            user.setCpf(req.cpf());
        }
        if (hasChanged(user.getWhatsapp(), req.whatsapp())) {
            changes.add("Zap: " + user.getWhatsapp() + " -> " + req.whatsapp());
            user.setWhatsapp(req.whatsapp());
        }

        if (req.role() != null && !req.role().isBlank()) {
            try {
                Role newRole = Role.valueOf(req.role());
                if (user.getRole() != newRole) {
                    changes.add("Cargo: " + user.getRole() + " -> " + newRole);
                    user.setRole(newRole);
                }
            } catch (IllegalArgumentException e) {
            }
        }

        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            changes.add("Senha alterada");
            user.setPassword(passwordEncoder.encode(req.newPassword()));
        }
        
        // --- AQUI ESTÁ A LÓGICA QUE VOCÊ PEDIU PARA ADICIONAR (MOVIMENTAÇÃO DE CONDOMÍNIO) ---
        if (req.tenantId() != null && !req.tenantId().isBlank()) {
            try {
                UUID newTenantUUID = UUID.fromString(req.tenantId());
                Tenant newTenant = tenantRepository.findById(newTenantUUID)
                        .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));

                boolean alreadyIn = user.getTenants().stream().anyMatch(t -> t.getId().equals(newTenantUUID));
                
                if (!alreadyIn) {
                    user.getTenants().add(newTenant);
                    user.setTenant(newTenant);
                    changes.add("Adicionado ao condomínio: " + newTenant.getNome());
                } else if (user.getTenant() == null || !user.getTenant().getId().equals(newTenantUUID)) {
                    user.setTenant(newTenant);
                    changes.add("Definido como condomínio principal: " + newTenant.getNome());
                }

            } catch (Exception e) {
                System.err.println("Erro ao vincular tenant: " + e.getMessage());
            }
        }

        userRepository.save(user);

        String details = changes.isEmpty() ? "Atualizou perfil (sem mudanças visíveis)"
                : "Alterou: " + String.join(", ", changes);
        logAction("EDITAR_USUARIO", "Em " + user.getNome() + ": " + details);
    }

    private void logAction(String action, String details) {
        try {
            User currentUser = getCurrentUser();
            AuditLog log = new AuditLog();
            log.setTimestamp(LocalDateTime.now().toString());
            log.setAction(action);
            log.setUserId(currentUser.getId().toString());
            log.setUserName(currentUser.getNome());
            log.setDetails(details);
            log.setResourceType("ADMIN_PANEL"); 
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Erro ao salvar log de auditoria: " + e.getMessage());
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
    }

    private UserDTO mapToDTO(User u) {
        String tenantName = "Sem Condomínio";
        UUID tenantId = null;
        
        if (!u.getTenants().isEmpty()) {
            Tenant t = u.getTenants().get(0);
            tenantName = t.getNome() + (u.getTenants().size() > 1 ? " (+" + (u.getTenants().size()-1) + ")" : "");
            tenantId = t.getId();
        }

        return new UserDTO(
                u.getId(), u.getNome(), u.getEmail(), u.getRole().name(),
                tenantName,
                u.getLastSeen(), null, u.getBloco(), u.getUnidade(),
                tenantId,
                u.getCpf(), u.getWhatsapp());
    }

    private boolean hasChanged(String oldVal, String newVal) {
        if (newVal == null) return false;
        if (oldVal == null) return !newVal.isEmpty();
        return !oldVal.equals(newVal);
    }
}