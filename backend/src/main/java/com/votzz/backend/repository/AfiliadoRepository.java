package com.votzz.backend.repository;

import com.votzz.backend.domain.Afiliado;
import com.votzz.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AfiliadoRepository extends JpaRepository<Afiliado, UUID> {
    
    // Busca um afiliado pelo código de indicação (ex: 'JOAO10')
    Optional<Afiliado> findByCodigoRef(String codigoRef);

    // [CORREÇÃO] Busca afiliado passando a entidade User completa
    // (Necessário para o AffiliateController funcionar como escrevemos)
    Optional<Afiliado> findByUser(User user);
    
    // Busca afiliado apenas pelo ID do usuário (Útil se você tiver apenas o UUID)
    Optional<Afiliado> findByUserId(UUID userId);
    
    // Verifica se já existe um código (Para validação no cadastro)
    boolean existsByCodigoRef(String codigoRef);
}