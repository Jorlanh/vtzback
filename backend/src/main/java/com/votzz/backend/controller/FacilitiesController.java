package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.integration.AsaasClient;
import com.votzz.backend.repository.*;
import com.votzz.backend.service.AuditService; 
import com.votzz.backend.service.FacilitiesService;
import com.votzz.backend.service.FileStorageService; 
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    
    // Injetamos o Service para delegar a lógica complexa de criação Híbrida
    private final FacilitiesService facilitiesService; 

    @GetMapping("/areas")
    public List<CommonArea> getAreas() {
        User user = getUser();
        if (user.getTenant() == null) return List.of();
        return areaRepository.findByTenantId(user.getTenant().getId());
    }

    // --- UPLOAD FOTO DA ÁREA ---
    @PostMapping(value = "/areas/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAreaPhoto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Arquivo vazio."));
        
        try {
            // CORRIGIDO: Passando "areas" como pasta
            String fileUrl = fileStorageService.uploadFile(file, "areas");
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao processar upload: " + e.getMessage()));
        }
    }

    // --- NOVO: UPLOAD COMPROVANTE DE RESERVA ---
    @PostMapping(value = "/bookings/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> uploadBookingReceipt(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        try {
            User user = getFreshUser();
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));

            // Segurança: Só o dono da reserva pode enviar
            if (!booking.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Acesso negado a esta reserva."));
            }

            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Arquivo inválido."));

            // CORRIGIDO: Passando "receipts" como pasta
            String fileUrl = fileStorageService.uploadFile(file, "receipts");
            
            // 2. Atualiza a reserva
            booking.setReceiptUrl(fileUrl);
            booking.setStatus("UNDER_ANALYSIS"); // Novo status: Em Análise (mantém bloqueado)
            
            bookingRepository.save(booking);

            auditService.log(user, booking.getTenant(), "UPLOAD_COMPROVANTE", 
                "Enviou comprovante para reserva de " + booking.getBookingDate(), "RESERVAS");

            return ResponseEntity.ok(Map.of("message", "Comprovante enviado! Reserva em análise.", "url", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro no upload: " + e.getMessage()));
        }
    }

    @PostMapping("/areas")
    @Transactional
    public ResponseEntity<?> createArea(@RequestBody CommonArea area) {
        User user = getFreshUser(); 
        
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADM_CONDO) {
            return ResponseEntity.status(403).body("Apenas síndicos ou administradores podem criar áreas.");
        }
        
        area.setTenant(user.getTenant());
        CommonArea savedArea = areaRepository.save(area);

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
        User user = getFreshUser(); 
        
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

    // --- LISTAGEM DE RESERVAS (CORRIGIDA) ---
    @GetMapping("/bookings")
    public List<Booking> getBookings() {
        User user = getUser();
        
        if (user.getTenant() == null) return List.of();

        // Busca TODAS as reservas do condomínio para poder bloquear o calendário
        List<Booking> allBookings = bookingRepository.findAllByTenantId(user.getTenant().getId());

        // PRIVACIDADE: Se não for síndico, oculta o nome dos vizinhos nas reservas
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADM_CONDO) {
            allBookings.forEach(b -> {
                if (!b.getUser().getId().equals(user.getId())) {
                    b.setNome("Reservado"); // Mascara o nome
                    b.setCpf(null);         // Remove CPF
                    b.setUnidade("---");    // Remove unidade
                    b.setReceiptUrl(null);  // Esconde comprovante
                }
            });
        }
        return allBookings;
    }

    // --- ENDPOINT PRINCIPAL DE CRIAÇÃO (AGORA DELEGA PARA O SERVICE HÍBRIDO) ---
    @PostMapping("/bookings")
    @Transactional
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        User user = getFreshUser(); 
        
        try {
            // Chamada ao Service que contém a lógica de:
            // 1. Validação de horário (30min rule)
            // 2. Escolha entre Asaas (Chave Condomínio) ou Pix Manual
            Map<String, Object> result = facilitiesService.createBooking(request, user);
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            // Retorna erro 400 com a mensagem de validação (ex: horário ocupado)
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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

    // --- NOVO ENDPOINT: VALIDAÇÃO MANUAL PELO SÍNDICO ---
    @PatchMapping("/bookings/{id}/validate")
    @Transactional
    public ResponseEntity<?> validateBooking(@PathVariable UUID id, @RequestBody Map<String, Boolean> payload) {
        User user = getFreshUser();
        
        // Apenas Síndico pode validar
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADM_CONDO) {
            return ResponseEntity.status(403).body("Apenas síndicos podem validar reservas.");
        }

        boolean isValid = payload.get("valid");
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));
        
        String oldStatus = booking.getStatus();
        String newStatus = isValid ? "CONFIRMED" : "REJECTED";
        
        booking.setStatus(newStatus);
        bookingRepository.save(booking);
        
        auditService.log(
            user, 
            booking.getTenant(), 
            "VALIDAR_RESERVA", 
            "Síndico " + (isValid ? "APROVOU" : "REJEITOU") + " a reserva #" + booking.getId(), 
            "RESERVAS"
        );
        
        return ResponseEntity.ok().build();
    }

    private User getUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private User getFreshUser() {
        User principal = getUser();
        return userRepository.findById(principal.getId()).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    public record BookingRequest(
        String areaId, String date, String startTime, String endTime,
        String nome, String cpf, String bloco, String unidade, String billingType
    ) {}
}