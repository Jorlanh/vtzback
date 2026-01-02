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

    // Injetando valores do application.properties (que podem vir do ambiente)
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
            System.out.println("----- INICIALIZANDO SUPER ADMIN (SEGURO) -----");

            // Verifica se o Admin já existe pelo E-mail
            User admin = userRepository.findByEmail(adminEmail).orElse(null);

            // Cria o objeto com os dados das variáveis de ambiente
            if (admin == null) {
                admin = new User();
                admin.setId(UUID.fromString(adminId)); // Usa o ID configurado
            }

            // Atualiza os dados (Garante que se você mudar a variável no servidor, o banco atualiza)
            admin.setNome(adminName);
            admin.setEmail(adminEmail);
            admin.setRole(Role.ADMIN);
            admin.setCpf(adminCpf);
            admin.setWhatsapp(adminPhone);

            // Só atualiza a senha se ela mudou (para não re-criptografar o mesmo hash toda vez)
            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                System.out.println(">>> Senha do Admin atualizada.");
            }

            userRepository.save(admin);
            System.out.println(">>> Super Admin verificado/atualizado com sucesso.");
        };
    }
}