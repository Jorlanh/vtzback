package com.votzz.backend.service;

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

    @Value("${votzz.admin.email}")
    private String masterEmail;

    // --- DASHBOARD ---
    public AdminDashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long onlineUsers = userRepository.countOnlineUsers(LocalDateTime.now().minusMinutes(5));
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByAtivoTrue();
        
        double mrrValue = tenantRepository.findByAtivoTrue().stream()
                .mapToDouble(t -> t.getPlano() != null ? t.getPlano().getPrecoBase().doubleValue() : 0.0)
                .sum();

        return new AdminDashboardStats(totalUsers, onlineUsers, totalTenants, activeTenants, mrrValue, 0L);
    }

    // --- LISTAGEM ORGANIZADA ---
    public Map<String, Object> listOrganizedUsers() {
        List<User> allUsers = userRepository.findAll();
        
        List<UserDTO> afiliados = allUsers.stream()
                .filter(u -> u.getRole() == Role.AFILIADO)
                .map(this::mapToDTO).collect(Collectors.toList());

        Map<String, List<UserDTO>> pastas = allUsers.stream()
                .filter(u -> u.getTenant() != null)
                .map(this::mapToDTO)
                .collect(Collectors.groupingBy(UserDTO::getCondominio));

        Map<String, Object> response = new HashMap<>();
        response.put("afiliados", afiliados);
        response.put("pastas", pastas);
        return response;
    }

    // --- NOVA FUNCIONALIDADE: LISTAR CONDOMÍNIOS PARA EDIÇÃO ---
    public List<Tenant> listAllTenants() {
        return tenantRepository.findAll();
    }

    // --- SUPORTE: EDIÇÃO TOTAL DE USUÁRIO ---
    @Transactional
    public void adminUpdateUser(UUID userId, UserDTO dto, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Atualiza dados básicos
        if (dto.getNome() != null) user.setNome(dto.getNome());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getCpf() != null) user.setCpf(dto.getCpf());
        if (dto.getWhatsapp() != null) user.setWhatsapp(dto.getWhatsapp());
        if (dto.getUnidade() != null) user.setUnidade(dto.getUnidade());
        if (dto.getBloco() != null) user.setBloco(dto.getBloco());

        // Troca de senha se solicitada
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
    }

    // --- SUPORTE: ALTERAR SECRET KEY DO CONDOMÍNIO ---
    @Transactional
    public void updateTenantSecret(UUID tenantId, String newSecret) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));
        
        tenant.setSecretKeyword(newSecret);
        tenantRepository.save(tenant);
    }

    // --- SEGURANÇA: EXCLUSÃO DE USUÁRIO (APENAS MESTRE) ---
    @Transactional
    public void deleteUser(UUID userId) {
        String currentAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Verifica se quem está tentando apagar é o Master definido no properties
        if (!currentAdmin.equalsIgnoreCase(masterEmail)) {
            throw new RuntimeException("PERMISSÃO NEGADA: Apenas o Admin Mestre (" + masterEmail + ") pode excluir contas.");
        }

        User target = userRepository.findById(userId).orElseThrow();
        
        // Impede que o mestre se auto-delete acidentalmente
        if (target.getEmail().equalsIgnoreCase(masterEmail)) {
            throw new RuntimeException("Você não pode excluir a conta Mestre do sistema.");
        }

        userRepository.delete(target);
    }

    // --- CRIAÇÃO E OUTROS MÉTODOS EXISTENTES ---
    private UserDTO mapToDTO(User u) {
        return new UserDTO(
            u.getId(), u.getNome(), u.getEmail(), u.getRole().name(),
            u.getTenant() != null ? u.getTenant().getNome() : "VOTZZ GLOBAL",
            u.getLastSeen(), u.getBloco(), u.getUnidade(),
            u.getTenant() != null ? u.getTenant().getId() : null,
            u.getCpf(), u.getWhatsapp()
        );
    }

    @Transactional
    public void createCoupon(String code, BigDecimal discount, Integer quantity) {
        for (int i = 0; i < quantity; i++) {
            Coupon coupon = new Coupon();
            coupon.setCode(quantity > 1 ? code.toUpperCase() + "-" + (i + 1) : code.toUpperCase());
            coupon.setDiscountPercent(discount);
            coupon.setActive(true);
            couponRepository.save(coupon);
        }
    }

    @Transactional
    public void createTenantManual(String condoName, String cnpj, Integer qtyUnits, String secretKeyword, 
                                   String nameSyndic, String emailSyndic, String cpfSyndic, String phoneSyndic, String passwordSyndic) {
        Plano plano = planoRepository.findAll().stream().findFirst().orElseThrow();
        Tenant tenant = new Tenant();
        tenant.setNome(condoName);
        tenant.setCnpj(cnpj);
        tenant.setUnidadesTotal(qtyUnits);
        tenant.setSecretKeyword(secretKeyword);
        tenant.setPlano(plano);
        tenant.setAtivo(true);
        tenantRepository.save(tenant);

        User syndic = new User();
        syndic.setNome(nameSyndic);
        syndic.setEmail(emailSyndic);
        syndic.setCpf(cpfSyndic);
        syndic.setWhatsapp(phoneSyndic);
        syndic.setPassword(passwordEncoder.encode(passwordSyndic));
        syndic.setRole(Role.SINDICO);
        syndic.setTenant(tenant);
        userRepository.save(syndic);
    }

    @Transactional
    public void createNewAdmin(String nome, String email, String cpf, String whatsapp, String password) {
        String current = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!current.equalsIgnoreCase(masterEmail)) throw new RuntimeException("Acesso Negado: Apenas o Mestre.");
        
        User admin = new User();
        admin.setNome(nome);
        admin.setEmail(email);
        admin.setCpf(cpf);
        admin.setWhatsapp(whatsapp);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }
}