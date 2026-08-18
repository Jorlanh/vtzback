package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.repository.PollRepository;
import com.votzz.backend.service.GovernanceService;
import com.votzz.backend.service.PollReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/governance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GovernanceController {

    private final GovernanceService governanceService;
    private final PollReportService pollReportService;
    private final UserRepository userRepository;
    private final PollRepository pollRepository;

    private User getFreshUser() {
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(governanceService.getDashboardData(getFreshUser()));
    }

    // --- DOWNLOAD DO PDF CORRIGIDO ---
    @GetMapping("/polls/{id}/report")
    public ResponseEntity<byte[]> downloadPollReport(@PathVariable UUID id) {
        
        // 1. Gera o Stream do PDF
        ByteArrayInputStream pdfStream = pollReportService.generatePollPdf(id);
        
        // 2. Converte para array de bytes
        byte[] bytes = pdfStream.readAllBytes(); 

        // 3. Pega nome do arquivo
        Poll poll = pollRepository.findById(id).orElseThrow();
        String filename = "Auditoria_" + poll.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";

        // 4. Retorna
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }

    // --- LEITURA DE COMUNICADO ---
    @PostMapping("/announcements/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable UUID id) {
        governanceService.markAnnouncementAsRead(id, getFreshUser().getId());
        return ResponseEntity.ok().build();
    }

    // --- VOTAÇÃO EM ENQUETE (ATUALIZADO PARA MULTI-UNIDADE) ---
    @PostMapping("/polls/{id}/vote")
    public ResponseEntity<?> votePoll(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        User user = getFreshUser();
        UUID optionId = UUID.fromString((String) payload.get("optionId"));
        
        // Extrai a lista de unidades do payload com segurança
        List<String> units = new ArrayList<>();
        if (payload.containsKey("units") && payload.get("units") instanceof List) {
            List<?> rawList = (List<?>) payload.get("units");
            for(Object o : rawList) {
                if (o != null) units.add(o.toString());
            }
        }

        // Chama o serviço passando a lista
        governanceService.votePoll(id, user, optionId, units);
        return ResponseEntity.ok().build();
    }

    // --- OUTROS ENDPOINTS ---
    
    @PostMapping("/announcements")
    public ResponseEntity<?> createAnnouncement(@RequestBody Announcement ann) {
        governanceService.createAnnouncement(ann, getFreshUser());
        return ResponseEntity.ok().build();
    }
    @PutMapping("/announcements/{id}")
    public ResponseEntity<?> updateAnnouncement(@PathVariable UUID id, @RequestBody Announcement ann) {
        governanceService.updateAnnouncement(id, ann, getFreshUser());
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable UUID id) {
        governanceService.deleteAnnouncement(id, getFreshUser());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/polls")
    public ResponseEntity<?> createPoll(@RequestBody Poll poll) {
        User user = getFreshUser();
        poll.setCreatedBy(user.getId());
        governanceService.createPoll(poll, user);
        return ResponseEntity.ok().build();
    }
    @PutMapping("/polls/{id}")
    public ResponseEntity<?> updatePoll(@PathVariable UUID id, @RequestBody Poll poll) {
        governanceService.updatePoll(id, poll, getFreshUser());
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/polls/{id}")
    public ResponseEntity<?> deletePoll(@PathVariable UUID id) {
        governanceService.deletePoll(id, getFreshUser());
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/events")
    public ResponseEntity<?> createCalendarEvent(@RequestBody CalendarEvent evt) {
        governanceService.createCalendarEvent(evt, getFreshUser());
        return ResponseEntity.ok().build();
    }
    @PutMapping("/events/{id}")
    public ResponseEntity<?> updateCalendarEvent(@PathVariable UUID id, @RequestBody CalendarEvent evt) {
        governanceService.updateCalendarEvent(id, evt, getFreshUser());
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/events/{id}")
    public ResponseEntity<?> deleteCalendarEvent(@PathVariable UUID id) {
        governanceService.deleteCalendarEvent(id, getFreshUser());
        return ResponseEntity.ok().build();
    }
}