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
        // Registra o interceptor para todas as rotas da API
        registry.addInterceptor(activityInterceptor)
                .addPathPatterns("/api/**") // Monitora apenas endpoints da API
                .excludePathPatterns("/api/auth/**"); // Opcional: Não monitorar rotas públicas de auth se não quiser
    }
}