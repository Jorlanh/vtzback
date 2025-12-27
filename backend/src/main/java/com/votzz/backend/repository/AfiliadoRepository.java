package com.votzz.backend.repository;

import com.votzz.backend.domain.Afiliado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AfiliadoRepository extends JpaRepository<Afiliado, UUID> {
    
    // Busca um afiliado pelo código de indicação (ex: 'JOAO10')
    Optional<Afiliado> findByCodigoRef(String codigoRef);
    
    // Busca afiliado pelo ID do usuário vinculado
    Optional<Afiliado> findByUserId(UUID userId);
    
    // Verifica se já existe um código
    boolean existsByCodigoRef(String codigoRef);
}