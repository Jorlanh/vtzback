package com.votzz.backend.config;

import com.votzz.backend.config.security.SecurityFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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

    private final SecurityFilter securityFilter;

    @Value("#{'${cors.allowed.origins}'.split(',')}")
    private List<String> allowedOrigins;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Rotas Públicas
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register-resident",
                    "/api/auth/register-affiliate",
                    "/api/auth/register-condo",
                    "/api/auth/condo-register",
                    "/api/auth/refresh",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/webhooks/**",       
                    "/api/tenants/public-list",
                    "/api/tenants/identifier/**", 
                    "/ws-votzz/**",           
                    "/h2-console/**",
                    "/error"
                ).permitAll()

                // Rotas específicas
                .requestMatchers(HttpMethod.GET, "/api/assemblies/*/dossier").authenticated()

                // Regras por Role
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/afiliado/**").hasAnyAuthority("ADMIN", "AFILIADO")

                // Dashboard e Financeiro (Permite visualização para Morador e gestão para outros)
                .requestMatchers(HttpMethod.GET, "/api/financial/**").hasAnyAuthority("ADMIN", "SINDICO", "ADM_CONDO", "MANAGER", "MORADOR")
                .requestMatchers("/api/financial/**").hasAnyAuthority("ADMIN", "SINDICO", "ADM_CONDO", "MANAGER")

                .requestMatchers("/api/tenants/audit-logs", "/api/tenants/bank-info").hasAnyAuthority("ADMIN", "SINDICO", "ADM_CONDO", "MANAGER")
                .requestMatchers("/api/tenants/**").hasAnyAuthority("ADMIN", "SINDICO", "ADM_CONDO", "MANAGER", "MORADOR")

                .requestMatchers("/api/users/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Remove espaços em branco das origens (prevenção contra erros no application.properties)
        List<String> cleanedOrigins = allowedOrigins.stream().map(String::trim).toList();
        config.setAllowedOrigins(cleanedOrigins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // IMPORTANTE: Adicionado X-Tenant-ID para permitir a comunicação multi-condomínio
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Tenant-ID",
            "X-Simulated-User",
            "asaas-access-token"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
