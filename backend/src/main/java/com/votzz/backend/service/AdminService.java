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
    private final PasswordEncoder passwordEncoder;

    @Value("${votzz.admin.email:admin@votzz.com}")
    private String masterEmail;

    // ID do Super Admin (God Mode)
    private static final String MASTER_ADMIN_ID = "10000000-0000-0000-0000-000000000000";

    // --- LISTAGENS ---
    public List<UserDTO> listAllAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<Tenant> listAllTenants() { 
        return tenantRepository.findAll(); 
    }
    
    public List<Coupon> listAllCoupons() { return couponRepository.findAll(); }

    public Map<String, Object> listOrganizedUsers() {
        List<User> allUsers = userRepository.findAll();
        
        List<UserDTO> afiliados = allUsers.stream()
            .filter(u -> u.getRole() == Role.AFILIADO)
            .map(this::mapToDTO)
            .collect(Collectors.toList());
            
        // Agrupa moradores por condomínio
        Map<String, List<UserDTO>> pastas = allUsers.stream()
            .filter(u -> u.getTenant() != null && u.getRole() != Role.ADMIN) 
            .map(this::mapToDTO)
            .collect(Collectors.groupingBy(dto -> dto.getCondominio() != null ? dto.getCondominio() : "Sem Condomínio"));
            
        Map<String, Object> resp = new HashMap<>();
        resp.put("afiliados", afiliados);
        resp.put("pastas", pastas);
        return resp;
    }

    // --- CRIAÇÃO DE USUÁRIO VINCULADO ---
    @Transactional
    public void createUserLinked(CreateUserRequest dto) {
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new RuntimeException("E-mail já está em uso.");
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
    }

    // --- CRIAÇÃO MANUAL DE CONDOMÍNIO ---
    @Transactional
    public void createTenantManual(ManualTenantDTO dto) {
        Plano plano = planoRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new RuntimeException("ERRO: Nenhum Plano cadastrado no banco."));

        if (userRepository.findByEmail(dto.emailSyndic()).isPresent()) {
            throw new RuntimeException("E-mail do síndico já cadastrado.");
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
    }

    // --- EDIÇÃO DE CONDOMÍNIO ---
    @Transactional
    public void updateTenant(UUID tenantId, UpdateTenantDTO dto) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        
        if (dto.nome() != null) tenant.setNome(dto.nome());
        if (dto.cnpj() != null) tenant.setCnpj(dto.cnpj());
        if (dto.unidadesTotal() != null) tenant.setUnidadesTotal(dto.unidadesTotal());
        if (dto.ativo() != null) tenant.setAtivo(dto.ativo());
        if (dto.secretKeyword() != null) tenant.setSecretKeyword(dto.secretKeyword());
        
        if (dto.cep() != null) tenant.setCep(dto.cep());
        if (dto.logradouro() != null) tenant.setLogradouro(dto.logradouro());
        if (dto.numero() != null) tenant.setNumero(dto.numero());
        if (dto.bairro() != null) tenant.setBairro(dto.bairro());
        if (dto.cidade() != null) tenant.setCidade(dto.cidade());
        if (dto.estado() != null) tenant.setEstado(dto.estado());
        if (dto.pontoReferencia() != null) tenant.setPontoReferencia(dto.pontoReferencia());

        tenantRepository.save(tenant);
    }

    // --- CUPONS ---
    @Transactional
    public void createCoupon(String code, BigDecimal discount, Integer quantity) {
        Coupon coupon = new Coupon();
        coupon.setCode(code.toUpperCase());
        coupon.setDiscountPercent(discount);
        coupon.setQuantity(quantity); 
        coupon.setActive(true);
        couponRepository.save(coupon);
    }

    @Transactional
    public void deleteCoupon(UUID id) { couponRepository.deleteById(id); }

    // --- ADMINS ---
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
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User target = userRepository.findById(userId).orElseThrow();
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail).orElseThrow();

        if (target.getId().toString().equals(MASTER_ADMIN_ID)) {
            throw new RuntimeException("O Super Admin Mestre não pode ser removido.");
        }

        if (target.getRole() == Role.ADMIN) {
            if (!currentUser.getId().toString().equals(MASTER_ADMIN_ID)) {
                throw new RuntimeException("Apenas o Super Admin pode remover outros administradores.");
            }
        }

        userRepository.delete(target);
    }

    // --- ATUALIZAÇÃO DE USUÁRIO (Corrigido para dados planos e WhatsApp) ---
    @Transactional
    public void adminUpdateUser(UUID userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId).orElseThrow();
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail).orElseThrow();

        // Proteção do Super Admin
        if (user.getId().toString().equals(MASTER_ADMIN_ID) && !currentUser.getId().toString().equals(MASTER_ADMIN_ID)) {
             throw new RuntimeException("Você não tem permissão para editar o Super Admin.");
        }

        // 1. Atualiza e-mail (com verificação de duplicidade)
        if (req.email() != null && !req.email().isBlank() && !req.email().equals(user.getEmail())) {
            if (userRepository.findByEmail(req.email()).isPresent()) {
                throw new RuntimeException("Este e-mail já está em uso.");
            }
            user.setEmail(req.email());
        }

        // 2. Atualiza campos simples (Nome, CPF, WhatsApp)
        // O Java acessa 'req.whatsapp()' porque definimos assim no Record,
        // mas o JSON pode vir como 'phone' ou 'celular' graças ao @JsonAlias no Controller.
        if(req.nome() != null && !req.nome().isBlank()) user.setNome(req.nome());
        if(req.cpf() != null) user.setCpf(req.cpf());
        if(req.whatsapp() != null) user.setWhatsapp(req.whatsapp());
        
        // 3. Atualiza senha se fornecida
        if(req.newPassword() != null && !req.newPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.newPassword()));
        }
        
        // 4. Salva no banco (IMPORTANTE)
        userRepository.save(user);
    }

    // --- DASHBOARD STATS ---
    public AdminDashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long onlineUsers = userRepository.countOnlineUsers(LocalDateTime.now().minusMinutes(5));
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByAtivoTrue();

        List<Tenant> paidTenants = tenantRepository.findByStatusAssinatura("PAID");
        BigDecimal mrr = BigDecimal.ZERO;

        for (Tenant t : paidTenants) {
            if (t.getPlano() != null && t.getPlano().getPrecoBase() != null) {
                BigDecimal preco = t.getPlano().getPrecoBase();
                if (t.getPlano().getCiclo() == Plano.Ciclo.ANUAL) {
                    mrr = mrr.add(preco.divide(new BigDecimal("12"), 2, RoundingMode.HALF_EVEN));
                } else {
                    mrr = mrr.add(preco.divide(new BigDecimal("3"), 2, RoundingMode.HALF_EVEN));
                }
            }
        }

        return new AdminDashboardStats(totalUsers, onlineUsers, totalTenants, activeTenants, mrr, 25);
    }

    private UserDTO mapToDTO(User u) {
        return new UserDTO(
            u.getId(), 
            u.getNome(), 
            u.getEmail(), 
            u.getRole().name(), 
            u.getTenant() != null ? u.getTenant().getNome() : "VOTZZ / Sem Condomínio", 
            u.getLastSeen(),
            null, 
            u.getBloco(), 
            u.getUnidade(), 
            u.getTenant() != null ? u.getTenant().getId() : null, 
            u.getCpf(), 
            u.getWhatsapp()
        );
    }
}