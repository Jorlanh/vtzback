package com.votzz.backend.config.security;

import com.votzz.backend.core.tenant.TenantContext; // Agora vai funcionar
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class TenantSecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Simulação de Header para Desenvolvimento
        String simulatedUser = request.getHeader("X-Simulated-User");
        String simulatedTenant = request.getHeader("X-Tenant-ID");

        if (simulatedUser != null && simulatedTenant != null) {
            try {
                // Configura o Tenant Context
                TenantContext.setTenant(UUID.fromString(simulatedTenant));

                // Simula autenticação do Spring Security
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        simulatedUser, 
                        null, 
                        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException e) {
                // Ignora se o UUID for inválido
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Sempre limpa o contexto após a requisição
            TenantContext.clear();
        }
    }
}