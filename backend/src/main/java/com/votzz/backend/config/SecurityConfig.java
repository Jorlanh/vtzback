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
            // CORREÇÃO: Desabilita CSRF globalmente para permitir POST/PUT/DELETE com JWT
            .csrf(csrf -> csrf.disable())
            
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // 1. Prioridade para OPTIONS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 2. Endpoints PÚBLICOS
                .requestMatchers(
                    "/api/auth/**",           
                    "/api/webhooks/**",       
                    "/api/tenants/public-list",
                    "/api/users/register-resident",
                    "/ws-votzz/**",           
                    "/h2-console/**"          
                ).permitAll()

                // 3. REGRAS PROTEGIDAS
                .requestMatchers("/api/payments/create-custom").authenticated() 
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN") 
                .requestMatchers("/api/afiliado/**").hasAnyAuthority("ADMIN", "AFILIADO")
                .requestMatchers("/api/tenants/**", "/api/financial/**").hasAnyAuthority("ADMIN", "SINDICO", "ADM_CONDO", "MANAGER")
                .requestMatchers("/api/users/**").authenticated()
                
                // 4. Fallback
                .anyRequest().authenticated()
            )
            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
            
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins); 
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID", "X-Simulated-User", "asaas-access-token"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}