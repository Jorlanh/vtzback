package com.votzz.backend.controller;

import com.votzz.backend.domain.User;
import com.votzz.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173") // Ajuste se seu vite rodar na 3000
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Login simples para demonstração (busca usuário pelo email)
    @PostMapping("/login-demo")
    public ResponseEntity<User> login(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }
}