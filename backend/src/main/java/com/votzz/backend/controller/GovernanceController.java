package com.votzz.backend.controller;

import com.votzz.backend.domain.*;
import com.votzz.backend.repository.UserRepository;
import com.votzz.backend.service.GovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/governance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GovernanceController {

    private final GovernanceService governanceService;
    private final UserRepository userRepository;

    private User getFreshUser() {
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(governanceService.getDashboardData(getFreshUser()));
    }

    // --- COMUNICADOS ---
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

    @PostMapping("/announcements/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable UUID id) {
        governanceService.markAnnouncementAsRead(id, getFreshUser().getId());
        return ResponseEntity.ok().build();
    }

    // --- ENQUETES (MICRO-DECISÕES) ---
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
    
    @PostMapping("/polls/{id}/vote")
    public ResponseEntity<?> votePoll(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        User user = getFreshUser();
        UUID optionId = UUID.fromString(payload.get("optionId"));
        governanceService.votePoll(id, user, optionId);
        return ResponseEntity.ok().build();
    }

    // PDF RELATÓRIO ENQUETE
    @GetMapping("/polls/{id}/report")
    public ResponseEntity<InputStreamResource> downloadPollReport(@PathVariable UUID id) {
        ByteArrayInputStream pdf = governanceService.generatePollReportPdf(id, getFreshUser());
        Poll poll = governanceService.getPollById(id); 
        
        String filename = "microdecisoes_" + poll.getTitle().replaceAll("\\s+", "_") + "_Votzz.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    // --- CALENDÁRIO ---
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