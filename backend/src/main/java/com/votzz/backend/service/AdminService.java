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

    // --- 1: AUDITORIA FILTRADA (Só Admins) ---
    public List<AuditLog> listAuditLogs() {
        return auditLogRepository.findAll().stream()
                .filter(log -> {
                    // Mantém se for explicitamente do painel de admin
                    if ("ADMIN_PANEL".equals(log.getResourceType())) return true;

                    // Fallback: Verifica se o usuário que fez a ação é um ADMIN da Votzz
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

    // --- 2: STATS E MRR (Cálculo Robusto) ---
    public AdminDashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long onlineUsers = userRepository.countOnlineUsers(LocalDateTime.now().minusMinutes(5));

        // Contagem bruta de condomínios
        long totalTenants = tenantRepository.count();

        // Contagem de ativos
        long activeTenants = tenantRepository.countByAtivoTrue();

        // Cálculo do MRR: Pega TODOS os ativos que têm plano
        List<Tenant> activeTenantsList = tenantRepository.findByAtivoTrue();

        BigDecimal mrr = BigDecimal.ZERO;

        for (Tenant t : activeTenantsList) {
            // Só soma se tiver plano e preço configurado
            if (t.getPlano() != null && t.getPlano().getPrecoBase() != null) {
                BigDecimal preco = t.getPlano().getPrecoBase();

                // Se o plano é anual, divide por 12 para achar o MRR (Monthly Recurring Revenue)
                if (t.getPlano().getCiclo() == Plano.Ciclo.ANUAL) {
                    mrr = mrr.add(preco.divide(new BigDecimal("12"), 2, RoundingMode.HALF_EVEN));
                } else {
                    // Assume mensal
                    mrr = mrr.add(preco);
                }
            }
        }

        return new AdminDashboardStats(totalUsers, onlineUsers, totalTenants, activeTenants, mrr, 25);
    }

    // --- MÉTODOS DE LISTAGEM ---

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
            String pastaKey = (u.getTenant() != null) ? u.getTenant().getNome() : "Sem Condomínio";
            pastas.computeIfAbsent(pastaKey, k -> new ArrayList<>()).add(dto);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("afiliados", afiliados);
        resp.put("pastas", pastas);
        return resp;
    }

    // --- CRIAÇÃO E EDIÇÃO ---

    @Transactional
    public void createUserLinked(CreateUserRequest dto) {
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new RuntimeException("E-mail já está em uso.");
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
        user.setTenant(tenant);
        user.setUnidade(dto.unidade());
        user.setBloco(dto.bloco());

        userRepository.save(user);
        logAction("CRIAR_USUARIO", "Criou usuário " + user.getNome() + " (" + user.getRole() + ") no condomínio " + tenant.getNome());
    }

    @Transactional
    public void createTenantManual(ManualTenantDTO dto) {
        Plano plano = planoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("ERRO: Nenhum Plano cadastrado no sistema."));

        if (userRepository.findByEmail(dto.emailSyndic()).isPresent())
            throw new RuntimeException("E-mail do síndico já cadastrado.");

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
        tenantRepository.save(tenant);

        User syndic = new User();
        syndic.setNome(dto.nameSyndic());
        syndic.setEmail(dto.emailSyndic());
        syndic.setCpf(dto.cpfSyndic());
        syndic.setWhatsapp(dto.phoneSyndic());
        syndic.setPassword(passwordEncoder.encode(dto.passwordSyndic()));
        syndic.setRole(Role.SINDICO);
        syndic.setTenant(tenant);
        syndic.setUnidade("ADM");
        userRepository.save(syndic);

        logAction("CRIAR_CONDOMINIO", "Criou condomínio " + tenant.getNome() + " e síndico " + syndic.getNome());
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

        userRepository.save(user);

        String details = changes.isEmpty() ? "Atualizou perfil (sem mudanças visíveis)"
                : "Alterou: " + String.join(", ", changes);
        logAction("EDITAR_USUARIO", "Em " + user.getNome() + ": " + details);
    }

    // --- UTILITÁRIOS ---

    private void logAction(String action, String details) {
        try {
            User currentUser = getCurrentUser();
            AuditLog log = new AuditLog();
            log.setTimestamp(LocalDateTime.now().toString());
            log.setAction(action);
            log.setUserId(currentUser.getId().toString());
            log.setUserName(currentUser.getNome());
            log.setDetails(details);
            log.setResourceType("ADMIN_PANEL"); // Tag essencial para o filtro
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
        return new UserDTO(
                u.getId(), u.getNome(), u.getEmail(), u.getRole().name(),
                u.getTenant() != null ? u.getTenant().getNome() : "Sem Condomínio",
                u.getLastSeen(), null, u.getBloco(), u.getUnidade(),
                u.getTenant() != null ? u.getTenant().getId() : null,
                u.getCpf(), u.getWhatsapp());
    }

    private boolean hasChanged(String oldVal, String newVal) {
        if (newVal == null) return false;
        if (oldVal == null) return !newVal.isEmpty();
        return !oldVal.equals(newVal);
    }
}