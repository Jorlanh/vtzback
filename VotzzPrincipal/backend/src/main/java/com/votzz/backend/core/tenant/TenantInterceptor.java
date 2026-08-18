package com.votzz.backend.core.tenant;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.exception.SubscriptionLockedException;
import com.votzz.backend.repository.TenantRepository;
import com.votzz.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 1. Libera rotas públicas, de autenticação e webhooks (pagamento)
        if (requestURI.startsWith("/api/auth") || 
            requestURI.startsWith("/api/webhooks") || 
            requestURI.startsWith("/api/public") ||
            requestURI.contains("swagger") || 
            requestURI.contains("api-docs")) {
            return true;
        }

        String tenantIdStr = request.getHeader(TENANT_HEADER);

        // Recuperação secundária de atributos
        if (tenantIdStr == null || tenantIdStr.isEmpty()) {
            Object tenantAttr = request.getAttribute("tenantId");
            if (tenantAttr != null) {
                tenantIdStr = tenantAttr.toString();
            }
        }

        if (tenantIdStr != null && !tenantIdStr.trim().isEmpty() && 
            !"null".equalsIgnoreCase(tenantIdStr) && !"undefined".equalsIgnoreCase(tenantIdStr)) {
            
            try {
                UUID tenantUuid = UUID.fromString(tenantIdStr.trim());
                
                // Busca o Tenant no banco para verificar status
                Tenant tenant = tenantRepository.findById(tenantUuid)
                        .orElseThrow(() -> new RuntimeException("Condomínio não encontrado."));

                // --- LÓGICA DA TRAVA DE 3 DIAS ---
                if (tenant.isBloqueadoPorPagamento()) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    
                    if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                        String email = auth.getName();
                        User user = userRepository.findByEmail(email).orElse(null);

                        if (user != null) {
                            // REGRA 1: Morador -> Bloqueio Total
                            if (user.getRole() == Role.MORADOR) {
                                throw new SubscriptionLockedException("O acesso ao condomínio está suspenso por pendência financeira. Contate a administração.");
                            }

                            // REGRA 2: Síndico/Admin -> Acesso Restrito (Só renovação)
                            if (user.getRole() == Role.SINDICO || user.getRole() == Role.ADMIN) {
                                // Permite apenas rotas de assinatura e dados básicos do usuário
                                boolean isAllowed = requestURI.startsWith("/api/subscription") || 
                                                    requestURI.equals("/api/users/me");

                                if (!isAllowed) {
                                    throw new SubscriptionLockedException("Plano Vencido. Realize a renovação para liberar o sistema.");
                                }
                            }
                        }
                    }
                }
                
                TenantContext.setTenant(tenantUuid);
                return true;

            } catch (IllegalArgumentException e) {
                logger.error("Tenant ID inválido: {}", tenantIdStr);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Tenant ID.");
                return false;
            }
        }

        return true; 
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}