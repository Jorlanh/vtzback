package com.votzz.backend.service;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.integration.AsaasClient;
import com.votzz.backend.repository.*;
import com.votzz.backend.controller.FacilitiesController.BookingRequest; // Import do Record

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacilitiesService {

    private final BookingRepository bookingRepository;
    private final CommonAreaRepository areaRepository;
    private final TenantPaymentConfigRepository paymentConfigRepository;
    private final UserRepository userRepository;
    private final AsaasClient asaasClient;
    private final AuditService auditService;

    // --- GESTÃO DE ÁREAS ---
    
    public CommonArea createArea(CommonArea area, User user) {
        if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADM_CONDO) {
            throw new RuntimeException("Acesso negado: Apenas síndicos podem criar áreas.");
        }
        area.setTenant(user.getTenant());
        CommonArea saved = areaRepository.save(area);
        
        auditService.log(user, user.getTenant(), "CRIAR_AREA", "Criou área: " + saved.getName(), "FACILITIES");
        return saved;
    }

    public CommonArea updateArea(UUID id, CommonArea details, User user) {
         if (user.getRole() != Role.SINDICO && user.getRole() != Role.ADM_CONDO) {
            throw new RuntimeException("Acesso negado.");
         }
         CommonArea area = areaRepository.findById(id).orElseThrow();
         
         if(details.getName() != null) area.setName(details.getName());
         if(details.getCapacity() != null) area.setCapacity(details.getCapacity());
         if(details.getPrice() != null) area.setPrice(details.getPrice());
         if(details.getDescription() != null) area.setDescription(details.getDescription());
         if(details.getImageUrl() != null) area.setImageUrl(details.getImageUrl());
         if(details.getOpenTime() != null) area.setOpenTime(details.getOpenTime());
         if(details.getCloseTime() != null) area.setCloseTime(details.getCloseTime());
         
         CommonArea updated = areaRepository.save(area);
         auditService.log(user, user.getTenant(), "ATUALIZAR_AREA", "Atualizou área: " + updated.getName(), "FACILITIES");
         return updated;
    }

    // --- CRIAÇÃO DE RESERVA (LÓGICA HÍBRIDA) ---

    @Transactional
    public Map<String, Object> createBooking(BookingRequest req, User user) {
        // 1. Converter e Validar
        UUID areaUuid = UUID.fromString(req.areaId());
        LocalDate dataReserva = LocalDate.parse(req.date());
        LocalTime inicio = LocalTime.parse(req.startTime());
        LocalTime fim = LocalTime.parse(req.endTime());

        if (dataReserva.isBefore(LocalDate.now())) throw new RuntimeException("Data inválida (passado).");
        if (inicio.isAfter(fim)) throw new RuntimeException("Horário inicial deve ser antes do final.");

        // 2. BUSCAR ENTIDADES
        CommonArea area = areaRepository.findById(areaUuid).orElseThrow(() -> new RuntimeException("Área não encontrada"));
        Tenant tenant = user.getTenant();
        
        // 3. BLOQUEIO INTELIGENTE (30 MINUTOS)
        List<Booking> conflitos = bookingRepository.findActiveBookingsByAreaAndDate(areaUuid, dataReserva);
        
        boolean horarioOcupado = conflitos.stream().anyMatch(b -> {
            // Ignora reservas canceladas, rejeitadas ou expiradas
            if ("EXPIRED".equals(b.getStatus()) || "CANCELLED".equals(b.getStatus()) || "REJECTED".equals(b.getStatus())) {
                return false; 
            }
            
            // Se for PENDING, verifica se ainda está dentro da janela de 30min
            if ("PENDING".equals(b.getStatus())) {
                // Se a reserva foi criada há mais de 30 min e ainda tá Pending, libera a vaga
                if (b.getCreatedAt().plusMinutes(30).isBefore(LocalDateTime.now())) {
                    return false; 
                }
            }
            
            // Verifica colisão de horário
            LocalTime bStart = LocalTime.parse(b.getStartTime());
            LocalTime bEnd = LocalTime.parse(b.getEndTime());
            // Lógica de intersecção de intervalos: (StartA < EndB) e (EndA > StartB)
            return inicio.isBefore(bEnd) && fim.isAfter(bStart);
        });

        if (horarioOcupado) {
            throw new RuntimeException("Horário indisponível. Já existe uma reserva ativa (ou aguardando pagamento) neste período.");
        }

        // 4. ATUALIZAR DADOS DO USUÁRIO (Snapshot)
        if (req.cpf() != null) user.setCpf(req.cpf());
        if (req.bloco() != null) user.setBloco(req.bloco());
        if (req.unidade() != null) user.setUnidade(req.unidade());
        userRepository.save(user);

        // 5. MONTAR RESERVA
        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCommonArea(area);
        booking.setUser(user);
        booking.setBookingDate(dataReserva);
        booking.setStartTime(req.startTime());
        booking.setEndTime(req.endTime());
        
        BigDecimal price = area.getPrice() != null ? area.getPrice() : BigDecimal.ZERO;
        booking.setTotalPrice(price);
        booking.setBillingType(req.billingType());
        
        booking.setNome(req.nome());
        booking.setCpf(req.cpf());
        booking.setUnidade(req.unidade());
        booking.setBloco(req.bloco());
        booking.setUnit(req.unidade());

        Map<String, Object> response = new HashMap<>();

        // 6. LÓGICA DE PAGAMENTO MULTI-TENANT
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            
            // Status inicial PENDING bloqueia o calendário por 30min
            booking.setStatus("PENDING"); 

            if ("ASAAS_PIX".equals(req.billingType())) {
                // --- FLUXO ASAAS AUTOMÁTICO (CHAVE DO CONDOMÍNIO) ---
                
                TenantPaymentConfig config = paymentConfigRepository.findByTenantId(tenant.getId())
                    .orElseThrow(() -> new RuntimeException("Condomínio não configurou o pagamento."));

                if (!config.isEnableAsaas() || config.getAsaasAccessToken() == null) {
                    throw new RuntimeException("Pagamento via Asaas não está habilitado neste condomínio.");
                }

                try {
                    // 6.1 Cria/Busca cliente no Asaas DO CONDOMÍNIO
                    String customerId = asaasClient.createCustomer(
                        booking.getNome(), 
                        booking.getCpf(), 
                        user.getEmail(), 
                        user.getWhatsapp(), 
                        config.getAsaasAccessToken() // <--- USA A CHAVE ESPECÍFICA
                    );
                    
                    // 6.2 Gera cobrança na conta DO CONDOMÍNIO
                    Map<String, Object> charge = asaasClient.createPixChargeForTenant(
                        customerId, 
                        price, 
                        "Reserva: " + area.getName() + " - " + req.date(), 
                        config.getAsaasAccessToken() // <--- USA A CHAVE ESPECÍFICA
                    );

                    booking.setAsaasPaymentId((String) charge.get("paymentId"));
                    
                    response.put("paymentId", charge.get("paymentId"));
                    response.put("encodedImage", charge.get("encodedImage"));
                    response.put("payload", charge.get("payload"));
                    response.put("message", "Cobrança Pix gerada. Pague em até 30 minutos para garantir a reserva.");

                } catch (Exception e) {
                    throw new RuntimeException("Erro ao gerar Pix no Asaas: " + e.getMessage());
                }

            } else {
                // --- FLUXO MANUAL (PIX DIRETO) ---
                response.put("message", "Reserva pré-agendada. Realize o Pix na conta do condomínio e envie o comprovante em até 30 minutos.");
            }
        } else {
            // Reserva Gratuita -> Aprova Direto ou Pendente de Aprovação do Síndico
            boolean needsApproval = Boolean.TRUE.equals(area.getRequiresApproval());
            booking.setStatus(needsApproval ? "PENDING" : "APPROVED"); // Se for grátis mas precisa aprovar, fica Pending (sem timer de pagamento)
            response.put("message", needsApproval ? "Aguardando aprovação do síndico." : "Reserva confirmada (Grátis).");
        }

        bookingRepository.save(booking);
        response.put("bookingId", booking.getId());
        
        auditService.log(user, tenant, "CRIAR_RESERVA", "Reservou " + area.getName(), "FACILITIES");

        return response;
    }
}