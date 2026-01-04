package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.integration.AsaasClient;
import com.votzz.backend.repository.*;
import com.votzz.backend.service.AuditService; 
import com.votzz.backend.service.FileStorageService; 
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final AuditService auditService; 
    private final FileStorageService fileStorageService; 

    @GetMapping("/areas")
    public List<CommonArea> getAreas() {
        User user = getUser();
        if (user.getTenant() == null) return List.of();
        return areaRepository.findByTenantId(user.getTenant().getId());
    }

    // --- UPLOAD (CORRIGIDO) ---
    @PostMapping(value = "/areas/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAreaPhoto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Arquivo vazio."));
        
        try {
            String fileUrl = fileStorageService.uploadFile(file);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao processar upload: " + e.getMessage()));
        }
    }

    @PostMapping("/areas")
    @Transactional
    public ResponseEntity<?> createArea(@RequestBody CommonArea area) {
        // CORREÇÃO CRÍTICA: Recarrega o usuário do banco para garantir que o Tenant não seja nulo/proxy
        User user = getFreshUser(); 
        
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADM_CONDO) {
            return ResponseEntity.status(403).body("Apenas síndicos ou administradores podem criar áreas.");
        }
        
        area.setTenant(user.getTenant());
        CommonArea savedArea = areaRepository.save(area);

        // Auditoria agora usará o user.getTenant() corretamente carregado
        auditService.log(
            user,
            user.getTenant(),
            "CRIAR_AREA",
            "Cadastrou nova área: " + savedArea.getName() + " (Capacidade: " + savedArea.getCapacity() + ")",
            "FACILITIES"
        );

        return ResponseEntity.ok(savedArea);
    }

    @PatchMapping("/areas/{id}")
    @Transactional
    public ResponseEntity<CommonArea> updateArea(@PathVariable UUID id, @RequestBody CommonArea areaDetails) {
        User user = getFreshUser(); // Recarrega usuário
        
        return areaRepository.findById(id).map(area -> {
            if (areaDetails.getName() != null) area.setName(areaDetails.getName());
            if (areaDetails.getCapacity() != null) area.setCapacity(areaDetails.getCapacity());
            if (areaDetails.getPrice() != null) area.setPrice(areaDetails.getPrice());
            if (areaDetails.getDescription() != null) area.setDescription(areaDetails.getDescription());
            if (areaDetails.getImageUrl() != null) area.setImageUrl(areaDetails.getImageUrl());
            if (areaDetails.getOpenTime() != null) area.setOpenTime(areaDetails.getOpenTime());
            if (areaDetails.getCloseTime() != null) area.setCloseTime(areaDetails.getCloseTime());
            
            CommonArea updated = areaRepository.save(area);

            auditService.log(
                user,
                updated.getTenant(),
                "EDITAR_AREA",
                "Atualizou dados da área: " + updated.getName(),
                "FACILITIES"
            );

            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/areas/{id}")
    public ResponseEntity<Void> deleteArea(@PathVariable UUID id) {
        areaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookings")
    public List<Booking> getBookings() {
        User user = getUser();
        if (user.getRole() == Role.SINDICO || user.getRole() == Role.ADM_CONDO) {
            return bookingRepository.findAllByTenantId(user.getTenant().getId());
        } else {
            return bookingRepository.findByUserIdOrderByBookingDateDesc(user.getId());
        }
    }

    @PostMapping("/bookings")
    @Transactional
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        User user = getFreshUser(); // Recarrega usuário
        Tenant tenant = user.getTenant();

        CommonArea area = areaRepository.findById(UUID.fromString(request.areaId()))
                .orElseThrow(() -> new RuntimeException("Área não encontrada"));

        LocalDate dataReserva = LocalDate.parse(request.date());
        if (dataReserva.isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body("Não é possível reservar datas passadas.");
        }

        LocalTime inicio = LocalTime.parse(request.startTime());
        LocalTime fim = LocalTime.parse(request.endTime());
        
        // Validações básicas de horário
        if (inicio.isAfter(fim)) return ResponseEntity.badRequest().body("Hora de início deve ser antes do fim.");

        List<Booking> conflitos = bookingRepository.findActiveBookingsByAreaAndDate(area.getId(), dataReserva);
        if (!conflitos.isEmpty()) {
            // Lógica simples de conflito (melhorar se necessário para checar horário)
            return ResponseEntity.badRequest().body("Esta data já está reservada.");
        }

        String cobrancaId = null;
        BigDecimal preco = area.getPrice();
        
        if (preco != null && preco.compareTo(BigDecimal.ZERO) > 0) {
            try {
                cobrancaId = asaasClient.criarCobrancaSplit(
                    tenant.getAsaasCustomerId(), preco, tenant.getAsaasWalletId(), BigDecimal.ZERO, request.billingType()
                );
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Erro financeiro: " + e.getMessage());
            }
        }

        // Atualiza dados cadastrais se vierem na requisição
        if (request.cpf() != null) user.setCpf(request.cpf());
        if (request.bloco() != null) user.setBloco(request.bloco());
        if (request.unidade() != null) user.setUnidade(request.unidade());
        userRepository.save(user);

        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCommonArea(area);
        booking.setUser(user);
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
        
        boolean isFree = preco == null || preco.compareTo(BigDecimal.ZERO) == 0;
        booking.setStatus(isFree ? "APPROVED" : "PENDING");

        bookingRepository.save(booking);

        auditService.log(
            user,
            tenant,
            "CRIAR_RESERVA",
            "Reservou " + area.getName() + " para " + dataReserva + " (" + request.startTime() + " - " + request.endTime() + ")",
            "RESERVAS"
        );

        return ResponseEntity.ok(Map.of("message", "Reserva criada!", "bookingId", booking.getId()));
    }

    @PatchMapping("/bookings/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(@PathVariable UUID id, @RequestBody String status) {
        User user = getFreshUser();
        return bookingRepository.findById(id).map(booking -> {
            String oldStatus = booking.getStatus();
            String newStatus = status.replace("\"", "");
            
            booking.setStatus(newStatus);
            Booking saved = bookingRepository.save(booking);

            auditService.log(
                user,
                booking.getTenant(),
                "ATUALIZAR_RESERVA",
                "Alterou status da reserva #" + booking.getId().toString().substring(0,8) + " de " + oldStatus + " para " + newStatus,
                "RESERVAS"
            );

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Método auxiliar para pegar o usuário do contexto (pode estar "Detached")
    private User getUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Método auxiliar para pegar o usuário do BANCO (Attached e com Tenant carregado)
    private User getFreshUser() {
        User principal = getUser();
        return userRepository.findById(principal.getId()).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    public record BookingRequest(
        String areaId, String date, String startTime, String endTime,
        String nome, String cpf, String bloco, String unidade, String billingType
    ) {}
}