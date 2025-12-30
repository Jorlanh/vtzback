package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.domain.enums.Role;
import com.votzz.backend.domain.enums.TicketPriority;
import com.votzz.backend.domain.enums.TicketStatus;
import com.votzz.backend.repository.TicketMessageRepository;
import com.votzz.backend.repository.TicketRepository;
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
        ticket.setPriority(TicketPriority.LOW); // Padrão é baixo, Admin altera depois
        
        return ticketRepository.save(ticket);
    }

    // --- ATUALIZAR STATUS ---
    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<Ticket> updateStatus(@PathVariable UUID id, @RequestBody StatusUpdateDTO update) {
        User user = getUser();
        return ticketRepository.findById(id).map(ticket -> {
            
            // Segurança: Morador só mexe no dele
            if (!isManager(user) && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).<Ticket>build();
            }

            ticket.setStatus(update.getStatus());
            return ResponseEntity.ok(ticketRepository.save(ticket));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- ADMIN DEFINE PRIORIDADE ---
    @PatchMapping("/{id}/priority")
    @Transactional
    public ResponseEntity<Ticket> updatePriority(@PathVariable UUID id, @RequestBody PriorityUpdateDTO update) {
        User user = getUser();
        
        if (!isManager(user)) {
            return ResponseEntity.status(403).build(); // Apenas Admin/Síndico
        }

        return ticketRepository.findById(id).map(ticket -> {
            ticket.setPriority(update.getPriority());
            return ResponseEntity.ok(ticketRepository.save(ticket));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- ENVIAR MENSAGEM NO CHAT ---
    @PostMapping("/{id}/messages")
    @Transactional
    public ResponseEntity<TicketMessage> addMessage(@PathVariable UUID id, @RequestBody MessageDTO messageDto) {
        User user = getUser();
        
        Ticket ticket = ticketRepository.findById(id).orElseThrow();

        // Validação: Só participa do chat o dono ou o admin
        if (!isManager(user) && !ticket.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        TicketMessage msg = new TicketMessage();
        msg.setTicket(ticket);
        msg.setMessage(messageDto.getMessage());
        msg.setSenderId(user.getId());
        msg.setSenderName(user.getNome());
        msg.setAdminSender(isManager(user));
        
        // Opcional: Se morador responde, muda status para IN_PROGRESS ou algo assim
        // Opcional: Se Admin responde, muda status para WAITING_TENANT

        return ResponseEntity.ok(messageRepository.save(msg));
    }

    // --- UTILITÁRIOS ---
    private User getUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean isManager(User user) {
        return user.getRole() == Role.SINDICO || user.getRole() == Role.ADM_CONDO || user.getRole() == Role.MANAGER;
    }

    // DTOs internos para não precisar criar arquivos soltos se não quiser
    @Data static class StatusUpdateDTO { private TicketStatus status; }
    @Data static class PriorityUpdateDTO { private TicketPriority priority; }
    @Data static class MessageDTO { private String message; }
}