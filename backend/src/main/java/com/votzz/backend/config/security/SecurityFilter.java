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

        if (token != null) {
            var login = tokenService.validateToken(token);

            if (login != null && !login.isEmpty()) {
                System.out.println("SecurityFilter - Email do token: " + login);

                // Busca todos os perfis com esse email
                List<User> users = userRepository.findByEmailIgnoreCase(login);
                System.out.println("SecurityFilter - Número de perfis encontrados: " + users.size());

                if (!users.isEmpty()) {
                    User selectedUser = null;

                    // Extrai tenantId do token, se existir
                    String tenantIdStr = tokenService.extractClaim(token, claims -> {
                        Object tid = claims.get("tenantId");
                        return tid != null ? tid.toString() : null;
                    });

                    UUID tenantIdFromToken = null; // Declarado fora para ser visível na lambda
                    if (tenantIdStr != null) {
                        try {
                            tenantIdFromToken = UUID.fromString(tenantIdStr);
                            System.out.println("SecurityFilter - TenantId extraído do token: " + tenantIdFromToken);
                        } catch (IllegalArgumentException e) {
                            System.out.println("SecurityFilter - TenantId inválido no token: " + tenantIdStr);
                        }
                    }

                    // Tenta encontrar um usuário com tenant compatível
                    if (tenantIdFromToken != null) {
                        final UUID finalTenantId = tenantIdFromToken; // final para uso na lambda
                        selectedUser = users.stream()
                                .filter(u -> u.getTenant() != null && finalTenantId.equals(u.getTenant().getId()))
                                .findFirst()
                                .orElse(null);
                    }

                    // Se não encontrou pelo tenant, pega o primeiro (comportamento original)
                    if (selectedUser == null) {
                        selectedUser = users.get(0);
                        System.out.println("SecurityFilter - Nenhum tenant compatível encontrado. Usando primeiro perfil.");
                    }

                    System.out.println("SecurityFilter - Usuário selecionado ID: " + selectedUser.getId());
                    System.out.println("SecurityFilter - Tenant do usuário selecionado: " +
                            (selectedUser.getTenant() != null ? selectedUser.getTenant().getId() : "NULL"));

                    // Coloca tenant no request para outros componentes usarem
                    if (selectedUser.getTenant() != null) {
                        request.setAttribute("tenantId", selectedUser.getTenant().getId().toString());
                    }

                    var authentication = new UsernamePasswordAuthenticationToken(
                            selectedUser, null, selectedUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    System.out.println("SecurityFilter - Nenhum usuário encontrado para email: " + login);
                }
            } else {
                System.out.println("SecurityFilter - Token inválido ou sem login");
            }
        } else {
            System.out.println("SecurityFilter - Nenhum token encontrado na requisição");
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}