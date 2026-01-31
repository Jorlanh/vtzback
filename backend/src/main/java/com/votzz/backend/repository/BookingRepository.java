package com.votzz.backend.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.votzz.backend.domain.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Busca reservas ativas para bloquear o dia.
    // ATUALIZADO: Adicionado 'EXPIRED' na lista de ignorados para liberar a data se o tempo acabar.
    @Query("SELECT b FROM Booking b WHERE b.commonArea.id = :areaId AND b.bookingDate = :date AND b.status NOT IN ('CANCELLED', 'REJECTED', 'CANCELED', 'EXPIRED')")
    List<Booking> findActiveBookingsByAreaAndDate(@Param("areaId") UUID areaId, @Param("date") LocalDate date);

    // Lista todas as reservas do condomínio (para o síndico)
    @Query("SELECT b FROM Booking b WHERE b.tenant.id = :tenantId ORDER BY b.bookingDate DESC")
    List<Booking> findAllByTenantId(@Param("tenantId") UUID tenantId);
    
    // Lista reservas de um usuário específico
    List<Booking> findByUserIdOrderByBookingDateDesc(UUID userId);
    
    // Busca pelo ID do pagamento do Asaas (para o Webhook)
    Booking findByAsaasPaymentId(String asaasPaymentId);

    // Busca reservas PENDENTES antigas para expirar automaticamente
    List<Booking> findAllByStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);

    // --- CORREÇÃO DO ERRO ---
    // O nome do método deve corresponder ao campo na entidade (asaasPaymentId)
    boolean existsByAsaasPaymentId(String asaasPaymentId);

}