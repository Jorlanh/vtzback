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

    // Captura o e-mail mascarado da sua configuração
    @Value("${votzz.admin.email}")
    private String masterEmail;

    public AdminDashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long onlineUsers = userRepository.countOnlineUsers(LocalDateTime.now().minusMinutes(5));
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByAtivoTrue();
        
        // MRR Real baseado nos planos ativos
        double mrrValue = tenantRepository.findByAtivoTrue().stream()
                .mapToDouble(t -> t.getPlano() != null ? t.getPlano().getPrecoBase().doubleValue() : 0.0)
                .sum();

        return new AdminDashboardStats(totalUsers, onlineUsers, totalTenants, activeTenants, mrrValue, 0L);
    }

    public Map<String, Object> listOrganizedUsers() {
        List<User> allUsers = userRepository.findAll();
        
        List<UserDTO> afiliados = allUsers.stream()
                .filter(u -> u.getRole() == Role.AFILIADO)
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        Map<String, List<UserDTO>> pastasCondominios = allUsers.stream()
                .filter(u -> u.getTenant() != null)
                .map(this::mapToDTO)
                .collect(Collectors.groupingBy(UserDTO::getCondominio));

        Map<String, Object> response = new HashMap<>();
        response.put("afiliados", afiliados);
        response.put("pastas", pastasCondominios);
        return response;
    }

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
        
        Plano plano = planoRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new RuntimeException("Cadastre ao menos um plano no banco primeiro."));

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
        syndic.setUnidade("ADM");
        userRepository.save(syndic);
    }

    @Transactional
    public void createNewAdmin(String nome, String email, String cpf, String whatsapp, String password) {
        String currentAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Proteção Mestre: Compara com o e-mail configurado
        if (!currentAdmin.equalsIgnoreCase(masterEmail)) {
            throw new RuntimeException("Acesso Negado: Apenas o Administrador Mestre pode criar novos Admins.");
        }
        
        User admin = new User();
        admin.setNome(nome);
        admin.setEmail(email);
        admin.setCpf(cpf);
        admin.setWhatsapp(whatsapp);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setTenant(null); 
        userRepository.save(admin);
    }

    @Transactional
    public void forceResetPassword(UUID userId, String newPassword) {
        User target = userRepository.findById(userId).orElseThrow();
        String currentAdmin = SecurityContextHolder.getContext().getAuthentication().getName();

        // Ninguém altera o mestre, exceto ele mesmo
        if (target.getEmail().equalsIgnoreCase(masterEmail) && !currentAdmin.equalsIgnoreCase(masterEmail)) {
            throw new RuntimeException("Proteção Mestre: Você não tem permissão para alterar este usuário.");
        }

        target.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(target);
    }
}