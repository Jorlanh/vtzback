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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. Tenta pegar do Header enviado pelo Frontend (api.ts atualizado acima)
        String tenantIdStr = request.getHeader(TENANT_HEADER);

        // 2. Fallback: Caso o SecurityFilter já tenha extraído do Token JWT e colocado nos atributos
        if (tenantIdStr == null || tenantIdStr.isEmpty()) {
            Object tenantAttr = request.getAttribute("tenantId");
            if (tenantAttr != null) {
                tenantIdStr = tenantAttr.toString();
            }
        }

        if (tenantIdStr != null && !tenantIdStr.isEmpty()) {
            try {
                UUID tenantUuid = UUID.fromString(tenantIdStr);
                TenantContext.setTenant(tenantUuid);
                // Log opcional para debug em desenvolvimento
                // logger.debug("Tenant definido no contexto: {}", tenantUuid);
            } catch (IllegalArgumentException e) {
                logger.error("Tenant ID inválido recebido: {}", tenantIdStr);
                // Opcional: response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Tenant ID");
                // return false;
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Limpa o contexto após a requisição para evitar vazamento de dados entre threads (ThreadLocal)
        TenantContext.clear();
    }
}