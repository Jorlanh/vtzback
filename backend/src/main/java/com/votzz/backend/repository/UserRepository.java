package com.votzz.backend.repository;

import com.votzz.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    // Busca um único (idealmente o Admin Global ou o primeiro encontrado)
    Optional<User> findByEmailOrCpf(String email, String cpf);
    
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);

    // NOVO: Busca TODOS os usuários com esse login (para o seletor de perfil)
    List<User> findAllByEmailOrCpf(String email, String cpf);

    List<User> findByTenantId(UUID tenantId);
    long countByTenantId(UUID tenantId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastSeen = :now WHERE u.email = :email")
    void updateLastSeen(@Param("email") String email, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastSeen > :limit")
    long countOnlineUsers(@Param("limit") LocalDateTime limit);
}