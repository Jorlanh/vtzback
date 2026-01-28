package com.votzz.backend.repository;

import com.votzz.backend.domain.Comissao;
import com.votzz.backend.domain.StatusComissao;
import com.votzz.backend.dto.TopAfiliadoDTO;
import org.springframework.data.domain.Pageable;
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

    /**
     * Busca comissões que estão BLOQUEADAS mas já venceram o prazo (dataLiberacao <= hoje).
     * Usado para mover de BLOQUEADO -> DISPONIVEL.
     */
    @Query("SELECT c FROM Comissao c WHERE c.afiliado.id = :afiliadoId AND c.status = 'BLOQUEADO' AND c.dataLiberacao <= :dataLimite")
    List<Comissao> findMatureCommissions(@Param("afiliadoId") UUID afiliadoId, @Param("dataLimite") LocalDate dataLimite);

    /**
     * Busca comissões DISPONÍVEIS para serem pagas (marcadas como PAGO).
     */
    List<Comissao> findByAfiliadoIdAndStatus(UUID afiliadoId, StatusComissao status);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Comissao c WHERE c.afiliado.id = :afiliadoId AND c.status = 'DISPONIVEL'")
    BigDecimal sumSaldoDisponivel(@Param("afiliadoId") UUID afiliadoId);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Comissao c WHERE c.afiliado.id = :afiliadoId AND c.status = 'BLOQUEADO'")
    BigDecimal sumSaldoFuturo(@Param("afiliadoId") UUID afiliadoId);
    
    @Query("SELECT new com.votzz.backend.dto.TopAfiliadoDTO(c.afiliado.user.nome, SUM(c.valor)) " +
           "FROM Comissao c GROUP BY c.afiliado.user.nome ORDER BY SUM(c.valor) DESC")
    List<TopAfiliadoDTO> findTopPerformers(Pageable pageable);
}