package com.votzz.backend.repository;

import com.votzz.backend.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Busca reservas ativas (PENDING ou APPROVED) para bloquear o dia
    @Query("SELECT b FROM Booking b WHERE b.commonArea.id = :areaId AND b.bookingDate = :date AND b.status NOT IN ('CANCELLED', 'REJECTED')")
    List<Booking> findActiveBookingsByAreaAndDate(@Param("areaId") UUID areaId, @Param("date") LocalDate date);

    // Lista todas as reservas do condomínio (para o síndico)
    @Query("SELECT b FROM Booking b WHERE b.tenant.id = :tenantId ORDER BY b.bookingDate DESC")
    List<Booking> findAllByTenantId(@Param("tenantId") UUID tenantId);
    
    // Lista reservas de um usuário específico
    List<Booking> findByUserIdOrderByBookingDateDesc(UUID userId);
    
    // Busca pelo ID do pagamento do Asaas (para o Webhook)
    Booking findByAsaasPaymentId(String asaasPaymentId);
}