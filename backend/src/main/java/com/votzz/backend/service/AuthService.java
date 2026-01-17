package com.votzz.backend.service;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.dto.AuthDTOs.*;
import com.votzz.backend.repository.*;
import com.votzz.backend.integration.AsaasClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PlanoRepository planoRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;
    private final AsaasClient asaasClient;
    private final TokenService tokenService; 

    @Value("${votzz.kiwify.link.essencial}")
    private String linkEssencial;

    @Value("${votzz.kiwify.link.business}")
    private String linkBusiness;

    public LoginResponse login(LoginRequest dto) {
        // 1. Busca o usuário
        User user = userRepository.findByEmail(dto.login())
                .or(() -> userRepository.findByCpf(dto.login()))
                .orElseThrow(() -> new RuntimeException("Usuário ou senha inválidos."));

        // 2. Valida Senha
        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new RuntimeException("Usuário ou senha inválidos.");
        }

        // 3. Valida Status
        if (!user.isEnabled()) {
            throw new RuntimeException("Conta desativada. Entre em contato com o suporte.");
        }

        // 4. Lógica do "Leque" (Multi-Tenant)
        List<Tenant> tenants = user.getTenants();
        boolean hasMultipleTenants = tenants != null && tenants.size() > 1;
        
        if (hasMultipleTenants && (dto.selectedProfileId() == null || dto.selectedProfileId().isBlank())) {
            List<ProfileOption> profiles = new ArrayList<>();
            for (Tenant t : tenants) {
                profiles.add(new ProfileOption(
                    t.getId().toString(), 
                    user.getRole().name(),
                    t.getNome(), 
                    user.getNome()
                ));
            }
            return new LoginResponse(true, profiles); 
        }

        // 5. Seleção do Contexto
        if (dto.selectedProfileId() != null && !dto.selectedProfileId().isBlank()) {
            UUID selectedTenantId = UUID.fromString(dto.selectedProfileId());
            Tenant selectedTenant = tenants.stream()
                    .filter(t -> t.getId().equals(selectedTenantId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Você não tem acesso a este condomínio."));
            user.setTenant(selectedTenant);
        } else if (tenants != null && !tenants.isEmpty()) {
            user.setTenant(tenants.get(0));
        }

        // --- NOVA LÓGICA: BUSCAR TODAS AS UNIDADES DESTE MORADOR NESTE CONDOMÍNIO ---
        // Isso resolve o problema de não aparecerem as opções no modal
        List<String> unidadesDoMorador = new ArrayList<>();
        
        if (user.getTenant() != null && user.getCpf() != null) {
            // Busca usuários com mesmo CPF e mesmo Tenant ID
            List<User> multiUnits = userRepository.findAll().stream()
                .filter(u -> u.getCpf() != null && u.getCpf().equals(user.getCpf()))
                .filter(u -> u.getTenant() != null && u.getTenant().getId().equals(user.getTenant().getId()))
                .toList();

            // Formata a string como o Frontend espera (ex: "Bloco A unidade 202")
            unidadesDoMorador = multiUnits.stream()
                .map(u -> {
                    String label = "";
                    if (u.getBloco() != null && !u.getBloco().isEmpty()) label += u.getBloco() + " ";
                    if (u.getUnidade() != null) label += "unidade " + u.getUnidade();
                    return label.trim();
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
        
        // Fallback: se a lista ficou vazia, adiciona ao menos a unidade do login atual
        if (unidadesDoMorador.isEmpty()) {
            String label = "";
            if (user.getBloco() != null) label += user.getBloco() + " ";
            if (user.getUnidade() != null) label += "unidade " + user.getUnidade();
            if (!label.isBlank()) unidadesDoMorador.add(label.trim());
        }

        // 6. Gera o Token
        String token = tokenService.generateToken(user, dto.keepLogged());
        
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        // 7. Retorna Sucesso com a lista preenchida
        return new LoginResponse(
            token, 
            "Bearer", 
            user.getId().toString(), 
            user.getNome(), 
            user.getEmail(), 
            user.getRole().name(), 
            user.getTenant() != null ? user.getTenant().getId().toString() : null,
            user.getTenant() != null ? user.getTenant().getNome() : "Sem Condomínio",
            user.getBloco(),   
            user.getUnidade(), 
            user.getCpf(),     
            unidadesDoMorador, // <--- LISTA PASSADA AQUI
            false, 
            false, 
            null 
        );
    }

    @Transactional
    public RegisterResponse registerCondo(RegisterCondoRequest dto) {
        if (userRepository.findByEmail(dto.emailSyndic()).isPresent()) {
            throw new RuntimeException("Este e-mail já está em uso.");
        }

        Plano plano = planoRepository.findById(UUID.fromString(dto.planId()))
            .orElseThrow(() -> new RuntimeException("Plano não encontrado."));

        Tenant tenant = new Tenant();
        tenant.setNome(dto.condoName());
        tenant.setCnpj(dto.cnpj());
        tenant.setUnidadesTotal(dto.qtyUnits());
        tenant.setBlocos(dto.qtyBlocks());
        tenant.setSecretKeyword(dto.secretKeyword());
        tenant.setPlano(plano);
        tenant.setAtivo(false);
        tenant.setStatusAssinatura("PENDING");

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
        syndic.setWhatsapp(dto.whatsappSyndic());
        syndic.setPassword(passwordEncoder.encode(dto.passwordSyndic()));
        syndic.setRole(Role.SINDICO);
        syndic.setTenant(tenant); 
        syndic.getTenants().add(tenant); 
        syndic.setUnidade("ADM");
        userRepository.save(syndic);

        String redirectUrl = null;
        String pixPayload = null;
        String pixImage = null;

        if (plano.getNome().equalsIgnoreCase("Custom")) {
            BigDecimal valorFinal = calcularValorCustom(dto.qtyUnits(), plano.getCiclo());

            if (dto.couponCode() != null && !dto.couponCode().trim().isEmpty()) {
                valorFinal = aplicarCupom(dto.couponCode(), valorFinal);
            }

            try {
                if (valorFinal.compareTo(BigDecimal.ZERO) > 0) {
                    String customerId = asaasClient.createCustomer(dto.nameSyndic(), dto.cpfSyndic(), dto.emailSyndic());
                    tenant.setAsaasCustomerId(customerId);
                    Map<String, Object> charge = asaasClient.createPixCharge(customerId, valorFinal);
                    if (charge != null) {
                        pixPayload = (String) charge.get("payload");
                        pixImage = (String) charge.get("encodedImage");
                    }
                    if (pixPayload == null || pixPayload.isEmpty()) {
                        throw new RuntimeException("O sistema financeiro não retornou o código Pix.");
                    }
                } else {
                    tenant.setStatusAssinatura("PAID");
                    tenant.setAtivo(true);
                }
                tenantRepository.save(tenant);
            } catch (Exception e) {
                throw new RuntimeException("Erro financeiro: " + e.getMessage());
            }

        } else {
            String nomePlano = plano.getNome().toLowerCase();
            String params = "?email=" + dto.emailSyndic() + "&name=" + dto.nameSyndic() + "&cpf=" + dto.cpfSyndic();
            if (nomePlano.contains("essencial")) redirectUrl = linkEssencial + params;
            else if (nomePlano.contains("business")) redirectUrl = linkBusiness + params;
        }

        return new RegisterResponse("Cadastro realizado!", redirectUrl, pixPayload, pixImage);
    }

    private BigDecimal aplicarCupom(String code, BigDecimal valorOriginal) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code)
            .orElseThrow(() -> new RuntimeException("Cupom inválido (" + code + ") ou não encontrado."));

        if (coupon.getQuantity() <= 0) throw new RuntimeException("Este cupom esgotou.");
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) throw new RuntimeException("Este cupom expirou.");

        BigDecimal desconto = valorOriginal.multiply(coupon.getDiscountPercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        BigDecimal novoValor = valorOriginal.subtract(desconto);

        coupon.setQuantity(coupon.getQuantity() - 1);
        if (coupon.getQuantity() <= 0) coupon.setActive(false);
        couponRepository.save(coupon);

        return novoValor.max(BigDecimal.ZERO);
    }

    private BigDecimal calcularValorCustom(int unidades, Plano.Ciclo ciclo) {
        BigDecimal basePrice = new BigDecimal("490.00");
        BigDecimal extraUnitCost = new BigDecimal("2.50");
        int franquia = 80;

        BigDecimal valorMensal = basePrice;
        if (unidades > franquia) {
            BigDecimal extras = BigDecimal.valueOf(unidades - franquia);
            valorMensal = valorMensal.add(extras.multiply(extraUnitCost));
        }

        if (ciclo == Plano.Ciclo.TRIMESTRAL) {
            return valorMensal.multiply(new BigDecimal("3"));
        } else {
            BigDecimal anualCheio = valorMensal.multiply(new BigDecimal("12"));
            BigDecimal desconto = anualCheio.multiply(new BigDecimal("0.20"));
            return anualCheio.subtract(desconto).setScale(2, RoundingMode.HALF_EVEN);
        }
    }

    public BigDecimal validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code).orElseThrow(() -> new RuntimeException("Cupom inválido ou inativo."));
        if (coupon.getQuantity() <= 0) throw new RuntimeException("Este cupom já foi totalmente utilizado.");
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) throw new RuntimeException("Este cupom expirou.");
        return coupon.getDiscountPercent();
    }
}