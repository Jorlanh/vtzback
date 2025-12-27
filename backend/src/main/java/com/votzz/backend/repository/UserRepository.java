package com.votzz.backend.repository;

import com.votzz.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    // Método para login
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
}