package com.votzz.backend.config;

import com.votzz.backend.config.security.TenantSecurityFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TenantSecurityFilter tenantSecurityFilter;

    public SecurityConfig(TenantSecurityFilter tenantSecurityFilter) {
        this.tenantSecurityFilter = tenantSecurityFilter;
    }

    // --- BEAN: Permite criptografia de senhas ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // 1. Rotas Públicas (Abertas para todos)
                .requestMatchers(
                    "/api/auth/**",              // Login e Recuperação de Senha
                    "/ws-votzz/**",              // WebSocket
                    "/h2-console/**",            // Banco de dados em memória (Dev)
                    "/api/reports/**",           // Relatórios públicos (se houver)
                    "/api/tenants/public-list"   // Lista de condomínios para cadastro
                ).permitAll()
                
                // 2. ROTA EXCLUSIVA DO SUPER ADMIN (Votzz)
                // Apenas usuários com role 'ADMIN' podem acessar /api/admin/**
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN") 
                
                // 3. Todo o resto exige autenticação (Token JWT válido)
                .anyRequest().authenticated()
            )
            .addFilterBefore(tenantSecurityFilter, UsernamePasswordAuthenticationFilter.class);
            
        // Permite o uso do H2 Console em iframes
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); 
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID", "X-Simulated-User"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}