package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.domain.enums.TicketPriority;
import com.votzz.backend.domain.enums.TicketStatus;
import com.votzz.backend.repository.TicketMessageRepository;
import com.votzz.backend.repository.TicketRepository;
import com.votzz.backend.service.AuditService; // IMPORTADO
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final AuditService auditService; // INJETADO PARA AUDITORIA

    // --- GET LISTA ---
    @GetMapping
    public List<Ticket> getTickets() {
        User user = getUser();
        if (isManager(user)) {
            return ticketRepository.findByTenantIdOrderByCreatedAtDesc(user.getTenant().getId());
        }
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    // --- CRIAR CHAMADO ---
    @PostMapping
    @Transactional
    public Ticket createTicket(@RequestBody Ticket ticket) {
        User user = getUser();
        
        ticket.setTenant(user.getTenant());
        ticket.setUserId(user.getId());
        
        // Dados snapshot
        if (ticket.getUserName() == null) ticket.setUserName(user.getNome());
        if (ticket.getUserUnit() == null) ticket.setUserUnit(user.getUnidade());
        if (ticket.getUserBlock() == null) ticket.setUserBlock(user.getBloco());
        
        // Padrões iniciais
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.LOW); 
        
        Ticket savedTicket = ticketRepository.save(ticket);

        // --- AUDITORIA DE CRIAÇÃO ---
        auditService.log(
            user,
            user.getTenant(),
            "ABRIR_CHAMADO",
            "Abriu chamado #" + savedTicket.getId() + ": " + savedTicket.getTitle(),
            "SUPORTE"
        );

        return savedTicket;
    }

    // --- ATUALIZAR STATUS (ASSUMIR, RESOLVER, ENCERRAR) ---
    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<Ticket> updateStatus(@PathVariable UUID id, @RequestBody StatusUpdateDTO update) {
        User user = getUser();
        return ticketRepository.findById(id).map(ticket -> {
            
            // Segurança: Morador só mexe no dele
            if (!isManager(user) && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).<Ticket>build();
            }

            TicketStatus oldStatus = ticket.getStatus();
            TicketStatus newStatus = update.getStatus();
            ticket.setStatus(newStatus);
            
            Ticket savedTicket = ticketRepository.save(ticket);

            // --- AUDITORIA DE STATUS (Inteligente) ---
            String acao = "ATUALIZAR_CHAMADO";
            String detalhe = "Status alterado de " + oldStatus + " para " + newStatus;

            // Personaliza a mensagem para ficar bonito no dashboard
            if (newStatus == TicketStatus.IN_PROGRESS) {
                acao = "ASSUMIR_CHAMADO";
                detalhe = "Assumiu o atendimento do chamado #" + id;
            } else if (newStatus == TicketStatus.RESOLVED) {
                acao = "RESOLVER_CHAMADO";
                detalhe = "Marcou como resolvido o chamado #" + id;
            } else if (newStatus == TicketStatus.CLOSED) {
                acao = "ENCERRAR_CHAMADO";
                detalhe = "Encerrou o chamado #" + id;
            }

            auditService.log(
                user,
                ticket.getTenant(), // Garante que logs de Admins Votzz apareçam para o síndico
                acao,
                detalhe,
                "SUPORTE"
            );

            return ResponseEntity.ok(savedTicket);
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- ADMIN DEFINE PRIORIDADE ---
    @PatchMapping("/{id}/priority")
    @Transactional
    public ResponseEntity<Ticket> updatePriority(@PathVariable UUID id, @RequestBody PriorityUpdateDTO update) {
        User user = getUser();
        
        if (!isManager(user)) {
            return ResponseEntity.status(403).build(); 
        }

        return ticketRepository.findById(id).map(ticket -> {
            TicketPriority oldPriority = ticket.getPriority();
            ticket.setPriority(update.getPriority());
            Ticket savedTicket = ticketRepository.save(ticket);

            // --- AUDITORIA DE PRIORIDADE ---
            auditService.log(
                user,
                ticket.getTenant(),
                "ALTERAR_PRIORIDADE",
                "Mudou prioridade do chamado #" + id + " de " + oldPriority + " para " + update.getPriority(),
                "SUPORTE"
            );

            return ResponseEntity.ok(savedTicket);
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- ENVIAR MENSAGEM NO CHAT ---
    @PostMapping("/{id}/messages")
    @Transactional
    public ResponseEntity<TicketMessage> addMessage(@PathVariable UUID id, @RequestBody MessageDTO messageDto) {
        User user = getUser();
        
        Ticket ticket = ticketRepository.findById(id).orElseThrow();

        // Validação
        if (!isManager(user) && !ticket.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        TicketMessage msg = new TicketMessage();
        msg.setTicket(ticket);
        msg.setMessage(messageDto.getMessage());
        msg.setSenderId(user.getId());
        msg.setSenderName(user.getNome());
        msg.setAdminSender(isManager(user));
        
        TicketMessage savedMsg = messageRepository.save(msg);

        // --- AUDITORIA DE RESPOSTA (Opcional, pode gerar muitos logs se o chat for intenso) ---
        // Se quiser auditar cada resposta, descomente abaixo:
        /*
        auditService.log(
            user,
            ticket.getTenant(),
            "RESPONDER_CHAMADO",
            "Nova mensagem no chamado #" + id,
            "SUPORTE"
        );
        */

        return ResponseEntity.ok(savedMsg);
    }

    // --- UTILITÁRIOS ---
    private User getUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean isManager(User user) {
        return user.getRole() == Role.SINDICO || user.getRole() == Role.ADM_CONDO || user.getRole() == Role.MANAGER || user.getRole() == Role.ADMIN;
    }

    // DTOs internos
    @Data static class StatusUpdateDTO { private TicketStatus status; }
    @Data static class PriorityUpdateDTO { private TicketPriority priority; }
    @Data static class MessageDTO { private String message; }
}