package com.votzz.backend.repository;

import com.votzz.backend.domain.Comissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComissaoRepository extends JpaRepository<Comissao, UUID> {

    // Lista comissões pendentes de pagamento (para o Cron Job)
    @Query("SELECT c FROM Comissao c WHERE c.afiliado.id = :afiliadoId AND c.status = 'BLOQUEADO' AND c.dataLiberacao <= :dataLimite")
    List<Comissao> findLiberadasParaSaque(@Param("afiliadoId") UUID afiliadoId, @Param("dataLimite") LocalDate dataLimite);

    // Soma saldo disponível (Real)
    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Comissao c WHERE c.afiliado.id = :afiliadoId AND c.status = 'DISPONIVEL'")
    BigDecimal sumSaldoDisponivel(@Param("afiliadoId") UUID afiliadoId);

    // Soma saldo futuro (Bloqueado)
    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Comissao c WHERE c.afiliado.id = :afiliadoId AND c.status = 'BLOQUEADO'")
    BigDecimal sumSaldoFuturo(@Param("afiliadoId") UUID afiliadoId);
    
    // Para o Dashboard Admin (Top Performers)
    @Query("SELECT new com.votzz.backend.dto.TopAfiliadoDTO(c.afiliado.user.nome, SUM(c.valor)) " +
           "FROM Comissao c GROUP BY c.afiliado.user.nome ORDER BY SUM(c.valor) DESC")
    List<com.votzz.backend.dto.TopAfiliadoDTO> findTopPerformers(org.springframework.data.domain.Pageable pageable);
}