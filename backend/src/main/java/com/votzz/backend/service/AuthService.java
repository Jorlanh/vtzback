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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PlanoRepository planoRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;
    private final AsaasClient asaasClient;

    @Value("${votzz.kiwify.link.essencial}")
    private String linkEssencial;

    @Value("${votzz.kiwify.link.business}")
    private String linkBusiness;

    @Transactional
    public RegisterResponse registerCondo(RegisterCondoRequest dto) {
        // 1. Validação de Email
        if (userRepository.findByEmail(dto.emailSyndic()).isPresent()) {
            throw new RuntimeException("Este e-mail já está em uso.");
        }

        // 2. Busca Plano
        Plano plano = planoRepository.findById(UUID.fromString(dto.planId()))
            .orElseThrow(() -> new RuntimeException("Plano não encontrado."));

        // 3. Cria Tenant
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

        // 4. Cria Síndico
        User syndic = new User();
        syndic.setNome(dto.nameSyndic());
        syndic.setEmail(dto.emailSyndic());
        syndic.setCpf(dto.cpfSyndic());
        syndic.setWhatsapp(dto.whatsappSyndic());
        syndic.setPassword(passwordEncoder.encode(dto.passwordSyndic()));
        syndic.setRole(Role.SINDICO);
        syndic.setTenant(tenant);
        syndic.setUnidade("ADM");
        userRepository.save(syndic);

        String redirectUrl = null;
        String pixPayload = null;
        String pixImage = null;

        // 5. Lógica Financeira (Custom vs Fixos)
        if (plano.getNome().equalsIgnoreCase("Custom")) {
            
            // A. Calcula Valor Base
            BigDecimal valorFinal = calcularValorCustom(dto.qtyUnits(), plano.getCiclo());

            // B. Aplica Cupom (Se enviado no cadastro final)
            if (dto.couponCode() != null && !dto.couponCode().trim().isEmpty()) {
                valorFinal = aplicarCupom(dto.couponCode(), valorFinal);
            }

            // C. Integração Asaas
            try {
                // Se valor > 0, gera cobrança
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
                    // 100% de Desconto (Valor Zero) -> Ativação Imediata
                    tenant.setStatusAssinatura("PAID");
                    tenant.setAtivo(true);
                    // Não gera Pix, retorna sucesso direto
                }

                tenantRepository.save(tenant);

            } catch (Exception e) {
                throw new RuntimeException("Erro financeiro: " + e.getMessage());
            }

        } else {
            // Planos Fixos (Kiwify)
            String nomePlano = plano.getNome().toLowerCase();
            String params = "?email=" + dto.emailSyndic() + "&name=" + dto.nameSyndic() + "&cpf=" + dto.cpfSyndic();
            
            if (nomePlano.contains("essencial")) redirectUrl = linkEssencial + params;
            else if (nomePlano.contains("business")) redirectUrl = linkBusiness + params;
        }

        return new RegisterResponse("Cadastro realizado!", redirectUrl, pixPayload, pixImage);
    }

    // Método privado para aplicar o desconto e DECREMENTAR o uso (chamado no registro)
    private BigDecimal aplicarCupom(String code, BigDecimal valorOriginal) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code)
            .orElseThrow(() -> new RuntimeException("Cupom inválido (" + code + ") ou não encontrado."));

        if (coupon.getQuantity() <= 0) {
            throw new RuntimeException("Este cupom esgotou.");
        }
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Este cupom expirou.");
        }

        BigDecimal desconto = valorOriginal.multiply(coupon.getDiscountPercent())
                                           .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        
        BigDecimal novoValor = valorOriginal.subtract(desconto);

        // Atualiza Cupom (Decrementa)
        coupon.setQuantity(coupon.getQuantity() - 1);
        if (coupon.getQuantity() <= 0) {
            coupon.setActive(false);
        }
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

    /**
     * [NOVO] Valida o cupom para o botão "Aplicar" do Frontend.
     * Apenas verifica e retorna a % sem decrementar quantidade.
     */
    public BigDecimal validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code)
            .orElseThrow(() -> new RuntimeException("Cupom inválido ou inativo."));

        if (coupon.getQuantity() <= 0) {
            throw new RuntimeException("Este cupom já foi totalmente utilizado.");
        }
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Este cupom expirou.");
        }

        return coupon.getDiscountPercent();
    }
}