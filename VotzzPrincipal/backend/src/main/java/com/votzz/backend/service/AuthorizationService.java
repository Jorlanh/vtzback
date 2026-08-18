package com.votzz.backend.service;

import com.votzz.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Procura por Email OU CPF
        // Como User agora implementa UserDetails, o return funciona direto.
        return userRepository.findByEmailOrCpf(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com e-mail ou CPF informado."));
    }
}