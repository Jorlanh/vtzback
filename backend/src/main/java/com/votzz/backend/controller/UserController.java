package com.votzz.backend.controller;

import com.votzz.backend.domain.User;
import com.votzz.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // Permite conexão do frontend
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Endpoint para atualizar dados do usuário (PATCH)
    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<String> updateUser(@PathVariable UUID id, @RequestBody UpdateUserDTO data) {
        // 1. Busca o usuário no banco
        var userOptional = userRepository.findById(id);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();

        // 2. Atualiza apenas os campos que foram enviados (não nulos)
        if (data.nome() != null && !data.nome().isEmpty()) {
            user.setNome(data.nome());
        }

        if (data.email() != null && !data.email().isEmpty()) {
            // Verifica se o novo e-mail já existe em OUTRA conta
            var userWithEmail = userRepository.findByEmail(data.email());
            if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body("Este e-mail já está em uso por outro usuário.");
            }
            user.setEmail(data.email());
        }

        if (data.whatsapp() != null) {
            user.setWhatsapp(data.whatsapp());
        }

        // 3. Tratamento especial para Senha (Criptografia)
        if (data.password() != null && !data.password().isBlank()) {
            String encryptedPassword = passwordEncoder.encode(data.password());
            user.setPassword(encryptedPassword);
        }

        // 4. Salva as alterações
        userRepository.save(user);

        return ResponseEntity.ok("Dados atualizados com sucesso!");
    }

    // Endpoint para listar todos (usado na Dashboard do Síndico)
    @GetMapping
    public ResponseEntity listAll() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // DTO interno para receber os dados
    public record UpdateUserDTO(String nome, String email, String whatsapp, String password) {}
}