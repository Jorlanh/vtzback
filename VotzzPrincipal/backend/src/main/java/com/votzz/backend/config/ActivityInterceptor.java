package com.votzz.backend.config;

import com.votzz.backend.domain.User;
import com.votzz.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ActivityInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Verifica se usuário está autenticado e não é anônimo
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                String email = auth.getName();
                Optional<User> userOpt = userRepository.findByEmail(email);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // Atualiza o lastSeen para AGORA
                    user.setLastSeen(LocalDateTime.now());
                    
                    // O save() garante que o Hibernate entenda que houve mudança e persista
                    userRepository.save(user); 
                }
            } catch (Exception e) {
                // Silencia erro para não bloquear a requisição
            }
        }
        return true;
    }
}