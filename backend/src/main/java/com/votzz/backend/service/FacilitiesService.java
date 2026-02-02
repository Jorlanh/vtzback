package com.votzz.backend.service;

import com.votzz.backend.domain.Booking;
import com.votzz.backend.domain.CommonArea;
import com.votzz.backend.domain.User;
import com.votzz.backend.dto.BookingRequest;
import com.votzz.backend.repository.BookingRepository;
import com.votzz.backend.repository.CommonAreaRepository;
import com.votzz.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacilitiesService {

    private final BookingRepository bookingRepository;
    private final CommonAreaRepository areaRepository;
    private final UserRepository userRepository;

    @Transactional
    public Booking createBooking(BookingRequest req) {
        // 1. Converter IDs e Datas
        UUID areaUuid = UUID.fromString(req.areaId());
        UUID userUuid = UUID.fromString(req.userId());
        LocalDate date = LocalDate.parse(req.date()); // Converte String para LocalDate

        // 2. Validar Conflitos
        List<Booking> existing = bookingRepository.findActiveBookingsByAreaAndDate(areaUuid, date);
        
        LocalTime newStart = LocalTime.parse(req.startTime());
        LocalTime newEnd = LocalTime.parse(req.endTime());
        
        if (newStart.isAfter(newEnd)) {
            throw new RuntimeException("A hora de início deve ser anterior ao fim.");
        }

        // Lógica simples de colisão de horário
        boolean hasConflict = existing.stream().anyMatch(b -> {
            LocalTime bStart = LocalTime.parse(b.getStartTime());
            LocalTime bEnd = LocalTime.parse(b.getEndTime());
            return newStart.isBefore(bEnd) && newEnd.isAfter(bStart);
        });

        if (hasConflict) {
            throw new RuntimeException("Horário indisponível. Já existe uma reserva neste intervalo.");
        }

        // 3. Buscar Entidades
        CommonArea area = areaRepository.findById(areaUuid)
                .orElseThrow(() -> new RuntimeException("Área comum não encontrada"));

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 4. Atualizar dados do usuário (opcional, mas bom para manter atualizado)
        if (req.whatsapp() != null) user.setWhatsapp(req.whatsapp()); // Se tiver campo no User
        if (req.cpf() != null) user.setCpf(req.cpf());
        userRepository.save(user);

        // 5. Criar Reserva
        Booking booking = new Booking();
        booking.setTenant(user.getTenant());
        booking.setCommonArea(area);
        booking.setUser(user);
        
        // Snapshot dos dados informados no formulário
        booking.setNome(req.nome());
        booking.setCpf(req.cpf());
        booking.setWhatsapp(req.whatsapp());
        booking.setBloco(req.block());
        booking.setUnidade(req.unit());
        booking.setUnit(req.unit()); // Compatibilidade
        
        booking.setBookingDate(date);
        booking.setStartTime(req.startTime());
        booking.setEndTime(req.endTime());
        booking.setTotalPrice(area.getPrice());
        booking.setBillingType(req.billingType());
        
        // Define status
        boolean isPaid = area.getPrice() != null && area.getPrice().compareTo(BigDecimal.ZERO) > 0;
        booking.setStatus(isPaid ? "PENDING" : "APPROVED");
        
        return bookingRepository.save(booking);
    }
}