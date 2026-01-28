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
                // === ROTAS PÚBLICAS ===
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/api/tenants/public-list").permitAll()
                .requestMatchers("/api/tenants/identifier/**").permitAll()
                .requestMatchers("/ws-votzz/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/error").permitAll()

                // === ROTAS FINANCEIRAS (Onde estava dando erro 403) ===
                // Verifica se o usuário tem QUALQUER UMA dessas roles
                .requestMatchers(HttpMethod.GET, "/api/financial/**").hasAnyAuthority("ADMIN", "SINDICO", "ADM_CONDO", "MANAGER", "MORADOR")
                .requestMatchers("/api/financial/**").hasAnyAuthority("ADMIN", "SINDICO", "ADM_CONDO", "MANAGER")

                // === ROTAS ADMINISTRATIVAS ===
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/afiliado/**").hasAnyAuthority("ADMIN", "AFILIADO")

                // === TENANTS E USERS ===
                .requestMatchers("/api/tenants/audit-logs", "/api/tenants/bank-info").hasAnyAuthority("ADMIN", "SINDICO", "ADM_CONDO", "MANAGER")
                .requestMatchers("/api/tenants/**").authenticated()
                .requestMatchers("/api/users/**").authenticated()
                
                // === ASSEMBLIES E ENCOMENDAS ===
                .requestMatchers("/api/assemblies/*/dossier").authenticated()
                .requestMatchers("/api/orders/**").authenticated()

                // === DEFAULT ===
                .anyRequest().authenticated()
            )
            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frame -> frame.disable())); // Para o H2 Console

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Tratamento para evitar erro se a lista vier nula ou vazia do properties
        if (allowedOrigins != null) {
            List<String> cleanedOrigins = allowedOrigins.stream().map(String::trim).toList();
            config.setAllowedOrigins(cleanedOrigins);
        } else {
            config.setAllowedOrigins(List.of("*")); // Fallback DEV
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID", "X-Simulated-User", "asaas-access-token"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}