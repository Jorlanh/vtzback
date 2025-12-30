package com.votzz.backend.repository;

import com.votzz.backend.domain.Plano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanoRepository extends JpaRepository<Plano, UUID> {
    
    // Método auxiliar opcional, caso queira buscar pelo nome (ex: "Business")
    Optional<Plano> findByNome(String nome);
    
}