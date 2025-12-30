package com.votzz.backend.config;

import com.votzz.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ActivityInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Verifica se há um usuário logado na sessão/token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            try {
                // Atualiza o lastSeen no banco
                String email = auth.getName();
                userRepository.updateLastSeen(email, LocalDateTime.now());
            } catch (Exception e) {
                // Silencia erro para não bloquear a requisição do usuário
                System.out.println("Erro ao atualizar lastSeen: " + e.getMessage());
            }
        }
        return true;
    }
}