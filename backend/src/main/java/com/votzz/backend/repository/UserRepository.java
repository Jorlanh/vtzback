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
    
    Optional<User> findByEmailOrCpf(String email, String cpf);
    Optional<User> findByEmail(String email);
    
    // Busca todos os perfis do usuário (Login)
    List<User> findByEmailIgnoreCase(String email);
    
    Optional<User> findByCpf(String cpf);
    
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);

    List<User> findAllByEmailOrCpf(String email, String cpf);

    // --- ALTERAÇÃO IMPORTANTE AQUI ---
    // Substituímos o método simples por uma Query que olha as duas tabelas.
    // Isso garante que Síndicos Profissionais (Multi-Tenant) e Moradores Comuns sejam encontrados.
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.tenants t WHERE u.tenant.id = :tenantId OR t.id = :tenantId")
    List<User> findByTenantId(@Param("tenantId") UUID tenantId);

    long countByTenantId(UUID tenantId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastSeen = :now WHERE u.email = :email")
    void updateLastSeen(@Param("email") String email, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastSeen >= :limit")
    long countOnlineUsers(@Param("limit") LocalDateTime limit);
}