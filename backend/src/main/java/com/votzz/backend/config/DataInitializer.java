package com.votzz.backend.config;

import com.votzz.backend.domain.Plano;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.repository.PlanoRepository;
import com.votzz.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
public class DataInitializer {

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
    CommandLineRunner initDatabase(UserRepository userRepository, PlanoRepository planoRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            System.out.println("----- INICIALIZANDO DADOS -----");

            // 1. GARANTE QUE OS PLANOS EXISTAM (SOMENTE TRIMESTRAL E ANUAL)
            inicializarPlanos(planoRepository);

            // 2. INICIALIZA SUPER ADMIN
            User admin = userRepository.findByEmail(adminEmail).orElse(null);

            if (admin == null) {
                admin = new User();
                try {
                    admin.setId(UUID.fromString(adminId));
                } catch (Exception e) {
                    admin.setId(UUID.randomUUID());
                }
            }

            admin.setNome(adminName);
            admin.setEmail(adminEmail);
            admin.setRole(Role.ADMIN);
            admin.setCpf(adminCpf);
            admin.setWhatsapp(adminPhone);

            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                System.out.println(">>> Senha do Admin atualizada.");
            }

            userRepository.save(admin);
            System.out.println(">>> Super Admin (" + adminEmail + ") pronto para uso.");
        };
    }

    private void inicializarPlanos(PlanoRepository repo) {
        // ESSENCIAL (Até 30 unidades)
        createPlanIfNotExist(repo, "ESSENCIAL_TRIMESTRAL", Plano.Ciclo.TRIMESTRAL, new BigDecimal("597.00"), 30); // ex: 199/mês * 3
        createPlanIfNotExist(repo, "ESSENCIAL_ANUAL", Plano.Ciclo.ANUAL, new BigDecimal("1990.00"), 30);      // ex: desconto no anual

        // BUSINESS (31 a 80 unidades)
        createPlanIfNotExist(repo, "BUSINESS_TRIMESTRAL", Plano.Ciclo.TRIMESTRAL, new BigDecimal("1197.00"), 80);
        createPlanIfNotExist(repo, "BUSINESS_ANUAL", Plano.Ciclo.ANUAL, new BigDecimal("3990.00"), 80);

        // CUSTOM (Manual)
        createPlanIfNotExist(repo, "CUSTOM_TRIMESTRAL", Plano.Ciclo.TRIMESTRAL, BigDecimal.ZERO, 9999);
        createPlanIfNotExist(repo, "CUSTOM_ANUAL", Plano.Ciclo.ANUAL, BigDecimal.ZERO, 9999);
    }

    private void createPlanIfNotExist(PlanoRepository repo, String nome, Plano.Ciclo ciclo, BigDecimal preco, Integer maxUnidades) {
        if (repo.findByNomeIgnoreCase(nome).isEmpty()) {
            Plano p = new Plano();
            p.setNome(nome);
            p.setCiclo(ciclo);
            p.setPrecoBase(preco);
            p.setMaxUnidades(maxUnidades);
            p.setTaxaServicoReserva(BigDecimal.ZERO);
            repo.save(p);
            System.out.println(">>> Plano criado: " + nome);
        }
    }
}