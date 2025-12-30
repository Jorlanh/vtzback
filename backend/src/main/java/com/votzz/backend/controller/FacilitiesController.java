package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.integration.AsaasClient;
import com.votzz.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FacilitiesController {

    private final CommonAreaRepository areaRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AsaasClient asaasClient;

    // --- ÁREAS COMUNS (CRUD) ---

    @GetMapping("/areas")
    public List<CommonArea> getAreas() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.getTenant() == null) return List.of();
        return areaRepository.findByTenantId(user.getTenant().getId());
    }

    @PostMapping("/areas")
    @Transactional
    public ResponseEntity<?> createArea(@RequestBody CommonArea area) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADM_CONDO) {
            return ResponseEntity.status(403).body("Apenas síndicos ou administradores podem criar áreas.");
        }
        
        area.setTenant(user.getTenant());
        return ResponseEntity.ok(areaRepository.save(area));
    }

    @PatchMapping("/areas/{id}")
    @Transactional
    public ResponseEntity<CommonArea> updateArea(@PathVariable UUID id, @RequestBody CommonArea areaDetails) {
        return areaRepository.findById(id).map(area -> {
            if (areaDetails.getName() != null) area.setName(areaDetails.getName());
            if (areaDetails.getCapacity() != null) area.setCapacity(areaDetails.getCapacity());
            if (areaDetails.getPrice() != null) area.setPrice(areaDetails.getPrice());
            if (areaDetails.getDescription() != null) area.setDescription(areaDetails.getDescription());
            if (areaDetails.getImageUrl() != null) area.setImageUrl(areaDetails.getImageUrl());
            if (areaDetails.getOpenTime() != null) area.setOpenTime(areaDetails.getOpenTime());
            if (areaDetails.getCloseTime() != null) area.setCloseTime(areaDetails.getCloseTime());
            return ResponseEntity.ok(areaRepository.save(area));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/areas/{id}")
    public ResponseEntity<Void> deleteArea(@PathVariable UUID id) {
        areaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- RESERVAS (BOOKINGS) ---

    @GetMapping("/bookings")
    public List<Booking> getBookings() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // Se for gestão, vê todas do condomínio. Se for morador, vê só as suas.
        if (user.getRole() == Role.SINDICO || user.getRole() == Role.ADM_CONDO) {
            return bookingRepository.findAllByTenantId(user.getTenant().getId());
        } else {
            return bookingRepository.findByUserIdOrderByBookingDateDesc(user.getId());
        }
    }

    @PostMapping("/bookings")
    @Transactional
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Tenant tenant = user.getTenant();

        // 1. Validar Área
        CommonArea area = areaRepository.findById(UUID.fromString(request.areaId()))
                .orElseThrow(() -> new RuntimeException("Área não encontrada"));

        // 2. Validar Data
        LocalDate dataReserva = LocalDate.parse(request.date());
        if (dataReserva.isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body("Não é possível reservar datas passadas.");
        }

        // 3. Validar Horário (08:00 as 22:00)
        LocalTime inicio = LocalTime.parse(request.startTime());
        LocalTime fim = LocalTime.parse(request.endTime());
        LocalTime limiteInicio = LocalTime.of(8, 0);
        LocalTime limiteFim = LocalTime.of(22, 0);

        if (inicio.isBefore(limiteInicio) || fim.isAfter(limiteFim)) {
            return ResponseEntity.badRequest().body("Horário permitido: 08:00 às 22:00.");
        }

        if (inicio.isAfter(fim)) {
             return ResponseEntity.badRequest().body("Hora de início deve ser antes do fim.");
        }

        // 4. Validar Disponibilidade
        List<Booking> conflitos = bookingRepository.findActiveBookingsByAreaAndDate(area.getId(), dataReserva);
        if (!conflitos.isEmpty()) {
            return ResponseEntity.badRequest().body("Esta data já está reservada para esta área.");
        }

        // 5. Gerar Cobrança no Asaas
        String cobrancaId = null;
        BigDecimal preco = area.getPrice();
        
        if (preco != null && preco.compareTo(BigDecimal.ZERO) > 0) {
            try {
                BigDecimal taxaServico = new BigDecimal("0.00"); 
                
                cobrancaId = asaasClient.criarCobrancaSplit(
                    tenant.getAsaasCustomerId(), 
                    preco,
                    tenant.getAsaasWalletId(),
                    taxaServico,
                    request.billingType()
                );
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Erro ao gerar cobrança: " + e.getMessage());
            }
        }

        // 6. Atualizar dados do usuário (opcional, mas bom para manter cadastro atualizado)
        if (request.cpf() != null) user.setCpf(request.cpf());
        if (request.bloco() != null) user.setBloco(request.bloco());
        if (request.unidade() != null) user.setUnidade(request.unidade());
        userRepository.save(user);

        // 7. Salvar Reserva
        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCommonArea(area);
        booking.setUser(user);
        
        // Dados do Snapshot
        booking.setNome(request.nome());
        booking.setCpf(request.cpf());
        booking.setBloco(request.bloco());
        booking.setUnidade(request.unidade());
        booking.setUnit(request.unidade()); 

        booking.setBookingDate(dataReserva);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setTotalPrice(preco);
        booking.setAsaasPaymentId(cobrancaId);
        booking.setBillingType(request.billingType());
        
        // Status inicial
        boolean isFree = preco == null || preco.compareTo(BigDecimal.ZERO) == 0;
        booking.setStatus(isFree ? "APPROVED" : "PENDING");

        bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of("message", "Reserva criada!", "bookingId", booking.getId()));
    }

    // [ADICIONADO] Funcionalidade que estava no BookingController
    @PatchMapping("/bookings/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(@PathVariable UUID id, @RequestBody String status) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus(status.replace("\"", ""));
            return ResponseEntity.ok(bookingRepository.save(booking));
        }).orElse(ResponseEntity.notFound().build());
    }

    public record BookingRequest(
        String areaId, 
        String date, 
        String startTime, 
        String endTime,
        String nome,
        String cpf,
        String bloco,
        String unidade,
        String billingType
    ) {}
}