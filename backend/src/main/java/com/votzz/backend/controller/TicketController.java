package com.votzz.backend.controller;

import com.votzz.backend.domain.Ticket;
import com.votzz.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketRepository repository;

    @GetMapping
    public List<Ticket> getAll() { 
        return repository.findAll(); 
    }

    @PostMapping
    public Ticket create(@RequestBody Ticket ticket) {
        ticket.setStatus("OPEN");
        return repository.save(ticket);
    }
}