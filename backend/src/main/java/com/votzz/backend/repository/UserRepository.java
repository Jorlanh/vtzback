package com.votzz.backend.repository;

import com.votzz.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    // [NOVO] Busca usuário por E-mail OU CPF (Fundamental para o login híbrido)
    Optional<User> findByEmailOrCpf(String email, String cpf);

    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);

    // [NOVO] Verifica se existe por CPF também (útil para validação no cadastro)
    boolean existsByCpf(String cpf);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastSeen = :now WHERE u.email = :email")
    void updateLastSeen(@Param("email") String email, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastSeen > :limit")
    long countOnlineUsers(@Param("limit") LocalDateTime limit);
}