package com.votzz.backend.config.security;

import com.votzz.backend.core.tenant.TenantContext;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var token = this.recoverToken(request);

        try {
            if (token != null) {
                var login = tokenService.validateToken(token);

                if (login != null && !login.isEmpty()) {
                    List<User> users = userRepository.findByEmailIgnoreCase(login);

                    if (!users.isEmpty()) {
                        User selectedUser = null;

                        // 1. Tenta pegar tenantId do token
                        String tenantIdStr = tokenService.extractClaim(token, claims -> {
                            Object tid = claims.get("tenantId");
                            return tid != null ? tid.toString() : null;
                        });

                        UUID tenantIdFromToken = null;
                        if (tenantIdStr != null) {
                            try {
                                tenantIdFromToken = UUID.fromString(tenantIdStr);
                            } catch (IllegalArgumentException e) {
                                // Ignora
                            }
                        }

                        // 2. Filtra o usuário correto
                        if (tenantIdFromToken != null) {
                            final UUID finalTenantId = tenantIdFromToken;
                            selectedUser = users.stream()
                                    .filter(u -> u.getTenant() != null && finalTenantId.equals(u.getTenant().getId()))
                                    .findFirst()
                                    .orElse(null);
                        }

                        // 3. Fallback
                        if (selectedUser == null) {
                            selectedUser = users.get(0);
                        }

                        // 4. Autenticação
                        var authentication = new UsernamePasswordAuthenticationToken(
                                selectedUser, null, selectedUser.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 5. INJEÇÃO DO CONTEXTO (CORRIGIDA)
                        if (selectedUser.getTenant() != null) {
                            UUID tenantId = selectedUser.getTenant().getId();
                            
                            // Define no Contexto Estático (CORRIGIDO PARA setTenant)
                            TenantContext.setTenant(tenantId);
                            
                            // Define nos atributos da requisição (para Controllers que usam @RequestAttribute)
                            request.setAttribute("tenantId", tenantId);
                            request.setAttribute("tenant-id", tenantId);
                            request.setAttribute("X-Tenant-ID", tenantId.toString());
                        }
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // Limpa o contexto para evitar vazamento de memória
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}