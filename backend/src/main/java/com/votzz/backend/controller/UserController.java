package com.votzz.backend.controller;

import com.votzz.backend.domain.Role;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.dto.ResidentRegisterRequest;
import com.votzz.backend.repository.TenantRepository;
import com.votzz.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. Cadastrar Morador (Com validação de segurança)
    @PostMapping("/register-resident")
    public ResponseEntity<User> registerResident(@RequestBody ResidentRegisterRequest request) {
        
        // A. Busca o Condomínio pelo ID selecionado
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado."));

        // B. VALIDAÇÃO DE SEGURANÇA (CNPJ + Palavra Chave)
        // Verifica se o CNPJ digitado bate com o do banco
        if (!tenant.getCnpj().equals(request.tenantCnpj().replaceAll("[^0-9]", ""))) { // Remove pontuação para comparar
            throw new RuntimeException("O CNPJ informado não corresponde ao condomínio selecionado.");
        }

        // Verifica a Palavra Secreta
        if (tenant.getSecretKeyword() == null || !tenant.getSecretKeyword().equals(request.secretKeyword())) {
            throw new RuntimeException("Palavra-chave do condomínio incorreta. Solicite ao Síndico.");
        }

        // C. Verifica se o e-mail já existe
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Este e-mail já está cadastrado.");
        }

        // D. Cria o Usuário
        User user = new User();
        user.setNome(request.nome());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCpf(request.cpf());
        user.setUnidade(request.unidade());
        user.setWhatsapp(request.whatsapp());
        user.setRole(Role.MORADOR);
        user.setTenant(tenant); // Vincula ao condomínio validado

        return ResponseEntity.ok(userRepository.save(user));
    }
}