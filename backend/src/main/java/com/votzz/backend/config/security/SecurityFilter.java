package com.votzz.backend.config.security;

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

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);

        if (token != null) {
            var login = tokenService.validateToken(token);

            if (login != null && !login.isEmpty()) {
                // CORREÇÃO PARA MULTI-TENANCY: Busca lista de perfis
                List<User> users = userRepository.findByEmailIgnoreCase(login);

                if (!users.isEmpty()) {
                    // Pega o primeiro perfil para autenticar a requisição no contexto do Spring
                    User user = users.get(0);

                    // Extrai o tenantId do token e coloca no request para o TenantInterceptor usar como fallback
                    String tenantId = tokenService.extractClaim(token, claims -> {
                        Object tid = claims.get("tenantId");
                        return tid != null ? tid.toString() : null;
                    });
                    
                    if (tenantId != null) {
                        request.setAttribute("tenantId", tenantId);
                    }

                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}