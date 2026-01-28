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
                        String headerTenantId = request.getHeader("X-Tenant-ID");
                        UUID targetTenantId = null;

                        // 1. Identifica qual Tenant o Frontend está pedindo
                        try {
                            if (headerTenantId != null && !headerTenantId.equals("null") && !headerTenantId.equals("undefined")) {
                                targetTenantId = UUID.fromString(headerTenantId);
                            }
                        } catch (Exception e) {
                            // Ignora erro de parse, targetTenantId fica null
                        }

                        // 2. Seleção de Contexto (Qual "versão" do usuário usar?)
                        if (targetTenantId != null) {
                            final UUID finalTid = targetTenantId;
                            // Busca o usuário específico que tem acesso a este tenant
                            selectedUser = users.stream()
                                .filter(u -> hasAccessToTenant(u, finalTid))
                                .findFirst()
                                .orElse(null);
                        }

                        // 3. Fallback Seguro: Se não pediu tenant específico, ou não achou, pega o padrão (primeiro da lista)
                        if (selectedUser == null) {
                            // CUIDADO: Em produção, idealmente você validaria se o usuário tem um "tenant default"
                            selectedUser = users.get(0); 
                            
                            // Se o usuário pediu um tenant X, mas não tinha acesso, e caiu aqui,
                            // o TenantContext abaixo vai garantir que ele não acesse dados errados
                            // pois vamos setar o tenant do selectedUser, não o do Header.
                        }

                        // --- DEBUG LOG (Remover em Produção) ---
                        System.out.println(">>> SECURITY FILTER <<<");
                        System.out.println("User: " + selectedUser.getEmail());
                        System.out.println("Tenant Solicitado: " + targetTenantId);
                        System.out.println("Tenant Definido no Contexto: " + (selectedUser.getTenant() != null ? selectedUser.getTenant().getId() : "Multi/Null"));
                        System.out.println("Authorities (Permissões): " + selectedUser.getAuthorities());
                        // ---------------------------------------

                        // 4. Autenticação no Spring Security
                        var authentication = new UsernamePasswordAuthenticationToken(
                                selectedUser, null, selectedUser.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 5. Injeção do Contexto do Tenant
                        // Prioridade: Tenant Solicitado (se válido) > Tenant do Usuário > Primeiro Tenant da Lista
                        if (targetTenantId != null && hasAccessToTenant(selectedUser, targetTenantId)) {
                            setTenantContext(request, targetTenantId);
                        } else if (selectedUser.getTenant() != null) {
                            setTenantContext(request, selectedUser.getTenant().getId());
                        } else if (selectedUser.getTenants() != null && !selectedUser.getTenants().isEmpty()) {
                            setTenantContext(request, selectedUser.getTenants().get(0).getId());
                        }
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean hasAccessToTenant(User u, UUID tenantId) {
        // Verifica vínculo único
        if (u.getTenant() != null && u.getTenant().getId().equals(tenantId)) {
            return true;
        }
        // Verifica vínculo lista (Many-to-Many)
        return u.getTenants() != null && u.getTenants().stream().anyMatch(t -> t.getId().equals(tenantId));
    }

    private void setTenantContext(HttpServletRequest request, UUID tenantId) {
        TenantContext.setTenant(tenantId);
        request.setAttribute("tenantId", tenantId);
        // Não sobrescreve o header da request original, mas garante o contexto interno
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}