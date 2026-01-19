package com.votzz.backend.config;

import com.votzz.backend.core.tenant.TenantInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ActivityInterceptor activityInterceptor;

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor de Atividade (Registra logs de ações)
        registry.addInterceptor(activityInterceptor)
                .addPathPatterns("/api/**") 
                .excludePathPatterns(
                    "/api/auth/**", 
                    "/api/payment/webhook/**",
                    "/api/tenants/public-list",
                    "/error"
                );

        // Interceptor de Tenant (Identifica o condomínio via X-Tenant-ID)
        // REGISTRAR APENAS UMA VEZ
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/auth/**",
                    "/api/tenants/public-list",
                    "/error"
                );
    }
}