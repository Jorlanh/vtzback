package com.votzz.backend.service;

import com.votzz.backend.domain.Booking;
import com.votzz.backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupService {

    private final BookingRepository bookingRepository;

    // Roda a cada 1 minuto (60000ms)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireOldBookings() {
        // Define o limite: Agora - 30 minutos
        LocalDateTime limit = LocalDateTime.now().minusMinutes(30);

        // Busca reservas que estão PENDENTES e foram criadas ANTES de 30 min atrás
        List<Booking> expiredBookings = bookingRepository.findAllByStatusAndCreatedAtBefore(
                "PENDING", limit 
        );

        if (!expiredBookings.isEmpty()) {
            log.info("Verificando {} reservas pendentes antigas...", expiredBookings.size());
        }

        for (Booking booking : expiredBookings) {
            // SALVA-VIDAS: Se o morador enviou o comprovante, NÃO expira, mesmo que esteja atrasado.
            // O status deveria ser UNDER_ANALYSIS, mas se por algum bug ficou PENDING com foto, a gente salva.
            boolean temComprovante = booking.getReceiptUrl() != null && !booking.getReceiptUrl().isEmpty();
            
            if (temComprovante) {
                // Opcional: Forçar status para análise para garantir
                booking.setStatus("UNDER_ANALYSIS");
                log.info("Reserva #{} salva da expiração (tinha comprovante).", booking.getId());
            } else {
                // Se não tem comprovante e passou 30 min -> EXPIRA
                booking.setStatus("EXPIRED");
                log.info("Reserva #{} expirada por falta de pagamento.", booking.getId());
            }
        }
        
        bookingRepository.saveAll(expiredBookings);
    }
}