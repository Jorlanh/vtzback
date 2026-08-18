package com.votzz.backend.service;

import com.votzz.backend.core.tenant.TenantContext;
import com.votzz.backend.domain.Guest;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.GuestStatus;
import com.votzz.backend.dto.GuestRequest;
import com.votzz.backend.repository.GuestRepository;
import com.votzz.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final UserRepository userRepository; 
    private final AuditService auditService; // INJETADO PARA REGISTRAR A AUDITORIA

    public List<Guest> findAllForCurrentTenant(String userEmail) {
        UUID tenantId = TenantContext.getCurrentTenant();
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String userRole = String.valueOf(user.getRole()).toUpperCase();

        if (userRole.contains("SINDICO") || 
            userRole.contains("ADM_CONDO") || 
            userRole.contains("MANAGER") || 
            userRole.contains("PORTEIRO") || 
            userRole.contains("ADMIN")) {
            return guestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        }
        
        return guestRepository.findByTenantIdAndResidentIdOrderByCreatedAtDesc(tenantId, user.getId());
    }

    @Transactional
    public Guest createGuest(GuestRequest request) {
        UUID tenantId = TenantContext.getCurrentTenant();
        
        Guest guest = Guest.builder()
                .tenantId(tenantId)
                .guestName(request.getGuestName())
                .guestRg(request.getGuestRg())
                .scheduledDate(request.getScheduledDate())
                .accessCode(UUID.randomUUID().toString())
                .status(GuestStatus.PENDING)
                .residentId(request.getResidentId())
                .residentName(request.getResidentName())
                .unit(request.getUnit())
                .block(request.getBlock())
                .residentWhatsapp(request.getResidentWhatsapp())
                .residentCpf(request.getResidentCpf())
                .residentEmail(request.getResidentEmail())
                .build();

        return guestRepository.save(guest);
    }

    @Transactional
    public Guest updateGuest(UUID id, GuestRequest request) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convidado não encontrado"));

        guest.setGuestName(request.getGuestName());
        guest.setGuestRg(request.getGuestRg());
        guest.setScheduledDate(request.getScheduledDate());
        guest.setResidentId(request.getResidentId());
        guest.setResidentName(request.getResidentName());
        guest.setUnit(request.getUnit());
        guest.setBlock(request.getBlock());
        guest.setResidentWhatsapp(request.getResidentWhatsapp());
        guest.setResidentCpf(request.getResidentCpf());
        guest.setResidentEmail(request.getResidentEmail());

        return guestRepository.save(guest);
    }

    @Transactional
    public void deleteGuest(UUID id) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convidado não encontrado"));
        
        guestRepository.delete(guest);
    }

    @Transactional
    public Guest authorizeEntry(UUID id, String userEmail) {
        // Quem está apertando o botão?
        User porteiro = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convidado não encontrado"));

        if (guest.getStatus() == GuestStatus.AUTHORIZED) {
            throw new RuntimeException("Este convidado já teve a entrada autorizada.");
        }

        guest.setStatus(GuestStatus.AUTHORIZED);
        guest.setEntryTime(LocalDateTime.now());

        Guest savedGuest = guestRepository.save(guest);

        // REGISTRO DE AUDITORIA DE SEGURANÇA
        String details = String.format("Acesso MANUAL autorizado para o visitante %s (RG: %s). Autorizado pelo funcionário %s. Morador responsável: %s (Bl %s Ap %s)",
                savedGuest.getGuestName(), savedGuest.getGuestRg(), porteiro.getNome(), savedGuest.getResidentName(), savedGuest.getBlock(), savedGuest.getUnit());
        auditService.log(porteiro, null, "ENTRADA_CONVIDADO_MANUAL", details, "GUESTS");

        return savedGuest;
    }

    @Transactional
    public Guest findAndAuthorizeByToken(String accessCode, String userEmail) {
        UUID tenantId = TenantContext.getCurrentTenant();
        
        User porteiro = userRepository.findByEmail(userEmail).orElse(null);

        Guest guest = guestRepository.findByAccessCodeAndTenantId(accessCode, tenantId)
                .orElseThrow(() -> new RuntimeException("Convite inválido ou expirado"));

        if (guest.getStatus() == GuestStatus.AUTHORIZED) {
            throw new RuntimeException("Este convidado já teve a entrada autorizada.");
        }

        guest.setStatus(GuestStatus.AUTHORIZED);
        guest.setEntryTime(LocalDateTime.now());

        Guest savedGuest = guestRepository.save(guest);

        // REGISTRO DE AUDITORIA DE SEGURANÇA
        if (porteiro != null) {
            String details = String.format("Acesso QR CODE liberado para o visitante %s (RG: %s). Leitura por %s. Morador responsável: %s (Bl %s Ap %s)",
                    savedGuest.getGuestName(), savedGuest.getGuestRg(), porteiro.getNome(), savedGuest.getResidentName(), savedGuest.getBlock(), savedGuest.getUnit());
            auditService.log(porteiro, null, "ENTRADA_CONVIDADO_QRCODE", details, "GUESTS");
        }

        return savedGuest;
    }
}