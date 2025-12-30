package com.votzz.backend.controller;

import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/renew")
    public ResponseEntity<String> renewSubscription(@RequestBody Map<String, Integer> payload) {
        // 1. Identifica quem está logado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        if (user.getTenant() == null) {
            return ResponseEntity.badRequest().body("Usuário não vinculado a um condomínio.");
        }

        // 2. Chama o serviço de renovação
        // Pega 'months' do payload ou assume 12 meses
        int months = payload.getOrDefault("months", 12);
        
        subscriptionService.renewSubscription(user.getTenant(), months);

        return ResponseEntity.ok("Assinatura renovada com sucesso!");
    }
}