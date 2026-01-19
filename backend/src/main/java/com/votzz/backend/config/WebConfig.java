package com.votzz.backend.config;

import com.votzz.backend.core.tenant.TenantInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ActivityInterceptor activityInterceptor;

    @Autowired
    private TenantInterceptor tenantInterceptor;

    // Lê a lista do seu application.properties: cors.allowed.origins
    @Value("#{'${cors.allowed.origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Configuração Global de CORS
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Cache da pré-requisição por 1 hora
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor de Atividade (Logs de ações do usuário)
        registry.addInterceptor(activityInterceptor)
                .addPathPatterns("/api/**") 
                .excludePathPatterns(
                    "/api/auth/**", 
                    "/api/payment/webhook/**",
                    "/api/tenants/public-list",
                    "/error"
                );

        // Interceptor de Tenant (CRUCIAL para identificar o condomínio em cada requisição)
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/auth/**",
                    "/api/tenants/public-list",
                    "/error"
                );
    }
}