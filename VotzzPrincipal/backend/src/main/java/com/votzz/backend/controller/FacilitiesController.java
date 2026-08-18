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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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

    // --- LISTAR ÁREAS ---
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
            String fileUrl = fileStorageService.uploadFile(file);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao processar upload: " + e.getMessage()));
        }
    }

    // --- UPLOAD COMPROVANTE DE RESERVA ---
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
            
            // Regra: Não permitir upload se expirado
            if ("EXPIRED".equals(booking.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tempo esgotado. Realize uma nova reserva."));
            }

            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Arquivo inválido."));

            // 1. Salva o arquivo
            String fileUrl = fileStorageService.uploadFile(file);
            
            // 2. Atualiza a reserva
            booking.setReceiptUrl(fileUrl);
            
            // Se estava pendente, muda para em análise
            if ("PENDING".equals(booking.getStatus())) {
                booking.setStatus("UNDER_ANALYSIS");
            }
            
            bookingRepository.save(booking);

            auditService.log(user, booking.getTenant(), "UPLOAD_COMPROVANTE", 
                "Enviou comprovante para reserva de " + booking.getBookingDate(), "RESERVAS");

            return ResponseEntity.ok(Map.of("message", "Comprovante enviado! Reserva em análise.", "url", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro no upload: " + e.getMessage()));
        }
    }

    // --- VALIDAR COMPROVANTE (SÍNDICO) ---
    // ESTE É O MÉTODO QUE FALTAVA PARA CORRIGIR O ERRO 500
    @PatchMapping("/bookings/{id}/validate")
    @Transactional
    public ResponseEntity<?> validateBooking(@PathVariable UUID id, @RequestBody Map<String, Boolean> payload) {
        try {
            User user = getFreshUser();
            
            // Apenas Admins podem validar (Síndico ou Adm Condo)
            if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADM_CONDO) {
                return ResponseEntity.status(403).body(Map.of("error", "Apenas síndicos podem validar comprovantes."));
            }

            Boolean isValid = payload.get("valid");
            if (isValid == null) return ResponseEntity.badRequest().body(Map.of("error", "Campo 'valid' é obrigatório."));

            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));

            String oldStatus = booking.getStatus();
            String newStatus = isValid ? "APPROVED" : "REJECTED"; // APPROVED = Confirmado
            
            booking.setStatus(newStatus);
            bookingRepository.save(booking);

            auditService.log(
                user,
                booking.getTenant(),
                "VALIDAR_RESERVA",
                "Alterou status de " + oldStatus + " para " + newStatus + " (Comprovante " + (isValid ? "Aceito" : "Recusado") + ")",
                "FINANCEIRO"
            );

            return ResponseEntity.ok(booking);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro interno: " + e.getMessage()));
        }
    }

    // --- CRUD ÁREAS (ADMIN) ---
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
            "Cadastrou nova área: " + savedArea.getName(),
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

    // --- LISTAGEM DE RESERVAS ---
    @GetMapping("/bookings")
    public List<Booking> getBookings() {
        User user = getUser();
        
        if (user.getTenant() == null) return List.of();

        // Busca TODAS as reservas do condomínio para poder bloquear o calendário
        List<Booking> allBookings = bookingRepository.findAllByTenantId(user.getTenant().getId());

        // PRIVACIDADE: Se não for síndico, oculta dados sensíveis dos vizinhos
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADM_CONDO && user.getRole() != Role.MANAGER) {
            allBookings.forEach(b -> {
                if (!b.getUser().getId().equals(user.getId())) {
                    b.setNome("Reservado"); // Mascara o nome
                    b.setCpf(null);         // Remove CPF
                    b.setUnidade("---");    // Remove unidade
                    b.setReceiptUrl(null);  // Esconde comprovante
                    b.setWhatsapp(null);    // Esconde Zap
                }
            });
        }
        return allBookings;
    }

    // --- CRIAÇÃO DE RESERVA (COM NOVO DTO) ---
    @PostMapping("/bookings")
    @Transactional
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        try {
            User user = getFreshUser(); 
            Tenant tenant = user.getTenant();

            // Validação básica
            if (request.areaId() == null) return ResponseEntity.badRequest().body("ID da área é obrigatório.");
            if (request.date() == null) return ResponseEntity.badRequest().body("Data é obrigatória.");

            CommonArea area = areaRepository.findById(UUID.fromString(request.areaId()))
                    .orElseThrow(() -> new RuntimeException("Área não encontrada"));

            // Parse de Data
            LocalDate dataReserva;
            try {
                dataReserva = LocalDate.parse(request.date());
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("Formato de data inválido.");
            }

            if (dataReserva.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body("Não é possível reservar datas passadas.");
            }

            LocalTime inicio = LocalTime.parse(request.startTime());
            LocalTime fim = LocalTime.parse(request.endTime());
            
            if (inicio.isAfter(fim)) return ResponseEntity.badRequest().body("Hora de início deve ser antes do fim.");

            // --- BLOQUEIO DE DATA ---
            List<Booking> conflitos = bookingRepository.findActiveBookingsByAreaAndDate(area.getId(), dataReserva);
            
            if (!conflitos.isEmpty()) {
                return ResponseEntity.badRequest().body("DATA INDISPONÍVEL: Já existe uma reserva ativa para este dia.");
            }

            // Integração Financeira
            String cobrancaId = null;
            BigDecimal preco = area.getPrice();
            
            if (preco != null && preco.compareTo(BigDecimal.ZERO) > 0 && "BOLETO".equalsIgnoreCase(request.billingType())) {
                try {
                    cobrancaId = asaasClient.criarCobrancaSplit(
                        tenant.getAsaasCustomerId(), preco, tenant.getAsaasWalletId(), BigDecimal.ZERO, "BOLETO"
                    );
                } catch (Exception e) {
                    System.err.println("Erro ao gerar boleto Asaas: " + e.getMessage());
                }
            }

            // --- ATUALIZAÇÃO DE DADOS CADASTRAIS DO USUÁRIO ---
            boolean userUpdated = false;
            
            // Atualiza CPF se enviado
            if (request.cpf() != null && !request.cpf().isEmpty()) { 
                user.setCpf(request.cpf()); 
                userUpdated = true; 
            }
            
            // Atualiza Telefone/WhatsApp se enviado
            if (request.whatsapp() != null && !request.whatsapp().isEmpty()) { 
                // Atualiza ambos os campos no User para garantir compatibilidade
                user.setPhone(request.whatsapp()); 
                user.setWhatsapp(request.whatsapp());
                userUpdated = true; 
            }
            
            // Mapeando block/bloco e unit/unidade
            String blocoFinal = request.block() != null ? request.block() : request.bloco();
            String unidadeFinal = request.unit() != null ? request.unit() : request.unidade();
            
            if (blocoFinal != null && !blocoFinal.isEmpty()) { user.setBloco(blocoFinal); userUpdated = true; }
            if (unidadeFinal != null && !unidadeFinal.isEmpty()) { user.setUnidade(unidadeFinal); userUpdated = true; }
            
            if (userUpdated) userRepository.save(user);

            // --- CRIAÇÃO DA ENTIDADE BOOKING ---
            Booking booking = new Booking();
            booking.setTenant(tenant);
            booking.setCommonArea(area);
            booking.setUser(user);
            
            // Snapshot dos dados no momento da reserva
            booking.setNome(request.nome() != null ? request.nome() : user.getNome());
            booking.setCpf(request.cpf());
            booking.setWhatsapp(request.whatsapp());
            booking.setBloco(blocoFinal);
            booking.setUnidade(unidadeFinal);
            booking.setUnit(unidadeFinal); // Compatibilidade legado
            
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
                "Reservou " + area.getName() + " para " + dataReserva,
                "RESERVAS"
            );

            return ResponseEntity.ok(Map.of("message", "Reserva criada!", "bookingId", booking.getId()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro interno ao processar reserva: " + e.getMessage());
        }
    }

    // --- ATUALIZAR STATUS (GENÉRICO) ---
    @PatchMapping("/bookings/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        User user = getFreshUser();
        String status = payload.get("status"); 

        return bookingRepository.findById(id).map(booking -> {
            String oldStatus = booking.getStatus();
            booking.setStatus(status);
            Booking saved = bookingRepository.save(booking);

            auditService.log(
                user,
                booking.getTenant(),
                "ATUALIZAR_RESERVA",
                "Alterou status da reserva de " + oldStatus + " para " + status,
                "RESERVAS"
            );

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- MÉTODOS AUXILIARES ---
    private User getUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) return (User) principal;
        throw new RuntimeException("Usuário não autenticado no contexto de segurança.");
    }

    private User getFreshUser() {
        User principal = getUser();
        return userRepository.findById(principal.getId()).orElseThrow(() -> new RuntimeException("Usuário não encontrado no banco."));
    }

    // --- DTO INTERNO (RECORD) ---
    public record BookingRequest(
        String areaId, 
        String userId,
        String date, 
        String startTime, 
        String endTime,
        String nome, 
        String cpf, 
        String whatsapp,
        String bloco, 
        String block,
        String unidade, 
        String unit,
        String billingType
    ) {}
}