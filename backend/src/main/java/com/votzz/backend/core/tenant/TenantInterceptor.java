package com.votzz.backend.core.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantIdStr = request.getHeader(TENANT_HEADER);

        // Recuperação secundária de atributos (útil para forwards internos)
        if (tenantIdStr == null || tenantIdStr.isEmpty()) {
            Object tenantAttr = request.getAttribute("tenantId");
            if (tenantAttr != null) {
                tenantIdStr = tenantAttr.toString();
            }
        }

        // Validação contra valores nulos ou strings de erro comuns do JS
        if (tenantIdStr != null && !tenantIdStr.trim().isEmpty() && 
            !"null".equalsIgnoreCase(tenantIdStr) && !"undefined".equalsIgnoreCase(tenantIdStr)) {
            try {
                UUID tenantUuid = UUID.fromString(tenantIdStr.trim());
                TenantContext.setTenant(tenantUuid);
                return true;
            } catch (IllegalArgumentException e) {
                logger.error("Tenant ID com formato UUID inválido: {}", tenantIdStr);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Tenant ID format.");
                return false; // Interrompe a requisição
            }
        }

        return true; // Prossegue se o tenant não for obrigatório para a rota
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear(); // Limpeza essencial para ThreadLocal
    }
}