package com.votzz.backend.config;

import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Configuration
public class DataInitializer {

    // Valores injetados do application.properties
    @Value("${votzz.admin.id}")
    private String adminId;

    @Value("${votzz.admin.email}")
    private String adminEmail;

    @Value("${votzz.admin.password}")
    private String adminPassword;

    @Value("${votzz.admin.nome}")
    private String adminName;

    @Value("${votzz.admin.cpf}")
    private String adminCpf;

    @Value("${votzz.admin.whatsapp}")
    private String adminPhone;

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            System.out.println("----- INICIALIZANDO SUPER ADMIN -----");

            // Verifica se o Admin já existe pelo E-mail
            User admin = userRepository.findByEmail(adminEmail).orElse(null);

            if (admin == null) {
                admin = new User();
                try {
                    admin.setId(UUID.fromString(adminId));
                } catch (Exception e) {
                    admin.setId(UUID.randomUUID());
                }
            }

            // Atualiza ou define os dados
            admin.setNome(adminName);
            admin.setEmail(adminEmail);
            admin.setRole(Role.ADMIN);
            admin.setCpf(adminCpf);
            admin.setWhatsapp(adminPhone);

            // Só re-criptografa se a senha mudou (evita logar com hash antigo)
            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                System.out.println(">>> Senha do Admin atualizada.");
            }

            userRepository.save(admin);
            System.out.println(">>> Super Admin (" + adminEmail + ") pronto para uso.");
        };
    }
}