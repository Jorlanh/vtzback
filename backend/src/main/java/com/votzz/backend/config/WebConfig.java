package com.votzz.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ActivityInterceptor activityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activityInterceptor)
                .addPathPatterns("/api/**") // Monitora apenas endpoints da API
                // Exclui rotas que não precisam de log de atividade do usuário
                .excludePathPatterns(
                    "/api/auth/**", 
                    "/api/payment/webhook/**",
                    "/api/tenants/public-list",
                    "/error"
                ); 
    }
}