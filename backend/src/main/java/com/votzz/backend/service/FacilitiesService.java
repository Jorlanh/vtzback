package com.votzz.backend.service;

import com.votzz.backend.domain.Booking;
import com.votzz.backend.domain.CommonArea;
import com.votzz.backend.domain.User;
import com.votzz.backend.dto.BookingRequest;
import com.votzz.backend.repository.BookingRepository;
import com.votzz.backend.repository.CommonAreaRepository;
import com.votzz.backend.repository.UserRepository; // Import novo
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacilitiesService {

    private final BookingRepository bookingRepository;
    private final CommonAreaRepository areaRepository;
    private final UserRepository userRepository; // Injeção necessária

    @Transactional
    public Booking createBooking(BookingRequest req) {
        // Converte as Strings vindas do DTO
        UUID areaUuid = UUID.fromString(req.areaId());
        UUID userUuid = UUID.fromString(req.userId());

        // 1. Validar Conflitos de Horário
        List<Booking> existing = bookingRepository.findActiveBookingsByAreaAndDate(areaUuid, req.date());
        
        LocalTime newStart = LocalTime.parse(req.startTime());
        LocalTime newEnd = LocalTime.parse(req.endTime());

        boolean hasConflict = existing.stream().anyMatch(b -> {
            LocalTime bStart = LocalTime.parse(b.getStartTime());
            LocalTime bEnd = LocalTime.parse(b.getEndTime());
            // Lógica de intersecção de horários
            return newStart.isBefore(bEnd) && newEnd.isAfter(bStart);
        });

        if (hasConflict) {
            throw new RuntimeException("Horário indisponível. Conflito com outra reserva.");
        }

        // 2. Buscar Entidades Reais (JPA exige a entidade, não só o ID)
        CommonArea area = areaRepository.findById(areaUuid)
                .orElseThrow(() -> new RuntimeException("Área comum não encontrada"));

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 3. Montar Objeto Booking
        Booking booking = new Booking();
        
        // CORREÇÃO: Usar os métodos que aceitam a Entidade
        booking.setCommonArea(area); // Antes estava setAreaId
        booking.setUser(user);       // Antes estava setUserId
        
        booking.setUnit(req.unit());
        booking.setBookingDate(req.date());
        booking.setStartTime(req.startTime());
        booking.setEndTime(req.endTime());
        booking.setTotalPrice(area.getPrice());
        
        // Define status
        boolean needsApproval = Boolean.TRUE.equals(area.getRequiresApproval());
        booking.setStatus(needsApproval ? "PENDING" : "APPROVED");
        
        return bookingRepository.save(booking);
    }
}