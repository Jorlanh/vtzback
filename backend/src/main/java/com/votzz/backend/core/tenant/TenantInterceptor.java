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
        String tenantIdStr = request.getHeader(TENANT_HEADER);

        if (tenantIdStr == null || tenantIdStr.isEmpty()) {
            Object tenantAttr = request.getAttribute("tenantId");
            if (tenantAttr != null) {
                tenantIdStr = tenantAttr.toString();
            }
        }

        if (tenantIdStr != null && !tenantIdStr.isEmpty() && !"null".equals(tenantIdStr)) {
            try {
                UUID tenantUuid = UUID.fromString(tenantIdStr);
                TenantContext.setTenant(tenantUuid);
            } catch (IllegalArgumentException e) {
                logger.error("Tenant ID inválido: {}", tenantIdStr);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}