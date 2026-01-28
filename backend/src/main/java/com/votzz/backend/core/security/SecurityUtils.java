package com.votzz.backend.core.security;

import com.votzz.backend.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        
        // Em alguns casos (jobs agendados), pode não ter usuário logado.
        throw new RuntimeException("Nenhum usuário autenticado encontrado no contexto.");
    }
}