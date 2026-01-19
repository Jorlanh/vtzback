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
import java.time.LocalDate;
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
    private final AfiliadoRepository afiliadoRepository;
    private final ComissaoRepository comissaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AsaasClient asaasClient;
    private final TokenService tokenService; 

    @Value("${votzz.kiwify.essencial.trimestral}")
    private String linkEssencialTrimestral;
    @Value("${votzz.kiwify.essencial.anual}")
    private String linkEssencialAnual;
    @Value("${votzz.kiwify.business.trimestral}")
    private String linkBusinessTrimestral;
    @Value("${votzz.kiwify.business.anual}")
    private String linkBusinessAnual;

    public LoginResponse login(LoginRequest dto) {
        List<User> candidates = userRepository.findAllByEmailOrCpf(dto.login(), dto.login());

        if (candidates.isEmpty()) {
            throw new RuntimeException("Usuário ou senha inválidos.");
        }

        List<User> validUsers = candidates.stream()
                .filter(u -> passwordEncoder.matches(dto.password(), u.getPassword()))
                .collect(Collectors.toList());

        if (validUsers.isEmpty()) {
            throw new RuntimeException("Usuário ou senha inválidos.");
        }

        User user = validUsers.get(0); 
        
        // Lógica do Leque
        if (validUsers.size() > 1 || (user.getTenants() != null && user.getTenants().size() > 1)) {
            if (dto.selectedProfileId() == null || dto.selectedProfileId().isBlank()) {
                List<ProfileOption> profiles = new ArrayList<>();
                for (User u : validUsers) {
                    if (u.getTenants().isEmpty()) {
                         profiles.add(new ProfileOption(u.getId().toString(), u.getRole().name(), "Perfil Global", u.getNome()));
                    } else {
                        for (Tenant t : u.getTenants()) {
                            profiles.add(new ProfileOption(t.getId().toString(), u.getRole().name(), t.getNome(), u.getNome()));
                        }
                    }
                }
                return new LoginResponse(true, profiles); 
            }
        }

        if (dto.selectedProfileId() != null && !dto.selectedProfileId().isBlank()) {
            UUID selectedTenantId = UUID.fromString(dto.selectedProfileId());
            boolean found = false;
            for(User u : validUsers) {
                Optional<Tenant> t = u.getTenants().stream().filter(tn -> tn.getId().equals(selectedTenantId)).findFirst();
                if(t.isPresent()) {
                    user = u;
                    user.setTenant(t.get());
                    found = true;
                    break;
                }
            }
            if(!found) throw new RuntimeException("Você não tem acesso a este condomínio.");

        } else if (user.getTenants() != null && !user.getTenants().isEmpty()) {
            user.setTenant(user.getTenants().get(0));
        }

        List<String> unidadesDoMorador = new ArrayList<>(); 
        if (user.getTenant() != null && user.getCpf() != null) {
            User finalUser = user;
            List<User> multiUnits = userRepository.findAllByEmailOrCpf(user.getEmail(), user.getCpf()).stream()
                .filter(u -> u.getTenant() != null && u.getTenant().getId().equals(finalUser.getTenant().getId()))
                .toList();
            unidadesDoMorador = multiUnits.stream().map(u -> {
                String label = "";
                if (u.getBloco() != null && !u.getBloco().isEmpty()) label += u.getBloco() + " ";
                if (u.getUnidade() != null) label += "unidade " + u.getUnidade();
                return label.trim();
            }).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        }
        
        if (unidadesDoMorador.isEmpty()) {
            String label = "";
            if (user.getBloco() != null) label += user.getBloco() + " ";
            if (user.getUnidade() != null) label += "unidade " + user.getUnidade();
            if (!label.isBlank()) unidadesDoMorador.add(label.trim());
        }

        String token = tokenService.generateToken(user, dto.keepLogged());
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        return new LoginResponse(
            token, "Bearer", user.getId().toString(), user.getNome(), user.getEmail(), 
            user.getRole().name(), 
            user.getTenant() != null ? user.getTenant().getId().toString() : null,
            user.getTenant() != null ? user.getTenant().getNome() : "Sem Condomínio",
            user.getBloco(), user.getUnidade(), user.getCpf(),     
            unidadesDoMorador, false, false, null 
        );
    }

    @Transactional
    public RegisterResponse registerCondo(RegisterCondoRequest dto) {
        Plano plano = planoRepository.findById(UUID.fromString(dto.planId()))
            .orElseThrow(() -> new RuntimeException("Plano não encontrado."));

        Optional<Tenant> existingTenantOpt = tenantRepository.findByCnpj(dto.cnpj());
        Tenant tenant;

        if (existingTenantOpt.isPresent()) {
            tenant = existingTenantOpt.get();
            if (tenant.isAtivo() || "PAID".equalsIgnoreCase(tenant.getStatusAssinatura())) {
                throw new RuntimeException("Este CNPJ já possui um cadastro ativo. Faça login.");
            }
        } else {
            tenant = new Tenant();
            tenant.setCnpj(dto.cnpj());
            tenant.setAtivo(false);
            tenant.setStatusAssinatura("PENDING");
        }

        tenant.setNome(dto.condoName());
        tenant.setUnidadesTotal(dto.qtyUnits());
        tenant.setBlocos(dto.qtyBlocks());
        tenant.setSecretKeyword(dto.secretKeyword());
        tenant.setPlano(plano);
        tenant.setCep(dto.cep());
        tenant.setLogradouro(dto.logradouro());
        tenant.setNumero(dto.numero());
        tenant.setBairro(dto.bairro());
        tenant.setCidade(dto.cidade());
        tenant.setEstado(dto.estado());
        tenant.setPontoReferencia(dto.pontoReferencia());

        tenant = tenantRepository.save(tenant);
        final Tenant finalTenant = tenant; 

        Optional<User> existingUserOpt = userRepository.findByEmail(dto.emailSyndic());
        User syndic;

        if (existingUserOpt.isPresent()) {
            syndic = existingUserOpt.get();
            if (!passwordEncoder.matches(dto.passwordSyndic(), syndic.getPassword())) {
                throw new RuntimeException("Este e-mail já existe. Senha incorreta.");
            }
            if (syndic.getTenants().stream().noneMatch(t -> t.getId().equals(finalTenant.getId()))) {
                syndic.getTenants().add(finalTenant);
            }
            syndic.setTenant(finalTenant);
        } else {
            syndic = new User();
            syndic.setNome(dto.nameSyndic());
            syndic.setEmail(dto.emailSyndic());
            syndic.setCpf(dto.cpfSyndic());
            syndic.setWhatsapp(dto.whatsappSyndic());
            syndic.setPassword(passwordEncoder.encode(dto.passwordSyndic()));
            syndic.setRole(Role.SINDICO);
            syndic.setTenant(finalTenant); 
            syndic.getTenants().add(finalTenant); 
            syndic.setUnidade("ADM");
        }
        userRepository.save(syndic);

        String redirectUrl = null;
        String pixPayload = null;
        String pixImage = null;

        String nomePlano = plano.getNome().toLowerCase();

        Afiliado afiliadoResponsavel = null;
        if (dto.affiliateCode() != null && !dto.affiliateCode().trim().isEmpty()) {
            Optional<Afiliado> afiliadoOpt = afiliadoRepository.findByCodigoRef(dto.affiliateCode());
            if (afiliadoOpt.isPresent()) {
                afiliadoResponsavel = afiliadoOpt.get();
            }
        }

        if (nomePlano.contains("custom")) {
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
                        
                        if (afiliadoResponsavel != null) {
                            BigDecimal valorComissao = valorFinal.multiply(new BigDecimal("0.20")); 
                            
                            Comissao comissao = new Comissao();
                            comissao.setAfiliado(afiliadoResponsavel);
                            
                            // CORREÇÃO DOS SETTERS PARA CONFORME A CLASSE COMISSAO.JAVA
                            comissao.setCondominioPagante(finalTenant); 
                            comissao.setValor(valorComissao);
                            comissao.setDataVenda(LocalDate.now()); 
                            comissao.setDataLiberacao(LocalDate.now().plusDays(30));
                            comissao.setStatus(StatusComissao.BLOQUEADO); 
                            
                            comissaoRepository.save(comissao);
                        }
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
            // Lógica Kiwify... (Mantida)
            String cycle = dto.cycle() != null ? dto.cycle().toUpperCase() : "ANUAL";
            String baseUrl = "";
            if (nomePlano.contains("essencial")) {
                baseUrl = "TRIMESTRAL".equals(cycle) ? linkEssencialTrimestral : linkEssencialAnual;
            } else if (nomePlano.contains("business")) {
                baseUrl = "TRIMESTRAL".equals(cycle) ? linkBusinessTrimestral : linkBusinessAnual;
            }
            
            String params = "?email=" + dto.emailSyndic() + "&name=" + dto.nameSyndic() + "&cpf=" + dto.cpfSyndic();
            if (dto.affiliateCode() != null) params += "&src=" + dto.affiliateCode(); 
            
            if (!baseUrl.isEmpty()) redirectUrl = baseUrl + params;
        }

        return new RegisterResponse("Cadastro realizado!", redirectUrl, pixPayload, pixImage);
    }

    private BigDecimal aplicarCupom(String code, BigDecimal valorOriginal) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code).orElse(null);
        if (coupon == null) return valorOriginal;
        if (coupon.getQuantity() <= 0) throw new RuntimeException("Este cupom esgotou.");
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) throw new RuntimeException("Este cupom expirou.");
        BigDecimal desconto = valorOriginal.multiply(coupon.getDiscountPercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        return valorOriginal.subtract(desconto).max(BigDecimal.ZERO);
    }

    private BigDecimal calcularValorCustom(int unidades, Plano.Ciclo ciclo) {
        BigDecimal basePrice = new BigDecimal("349.00"); 
        BigDecimal extraUnitCost = new BigDecimal("1.50");
        int franquia = 80;
        BigDecimal valorMensal = basePrice;
        if (unidades > franquia) {
            BigDecimal extras = BigDecimal.valueOf(unidades - franquia);
            valorMensal = valorMensal.add(extras.multiply(extraUnitCost));
        }
        if (ciclo == Plano.Ciclo.TRIMESTRAL) return valorMensal.multiply(new BigDecimal("3"));
        else return valorMensal.multiply(new BigDecimal("12")).multiply(new BigDecimal("0.8"));
    }
    
    public BigDecimal validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code).orElseThrow(() -> new RuntimeException("Cupom inválido."));
        return coupon.getDiscountPercent();
    }
}