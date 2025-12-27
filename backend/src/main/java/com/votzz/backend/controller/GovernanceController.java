package com.votzz.backend.controller;

import com.votzz.backend.domain.Announcement;
import com.votzz.backend.domain.AuditLog;
import com.votzz.backend.repository.AnnouncementRepository;
import com.votzz.backend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class GovernanceController {

    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @GetMapping("/announcements")
    public List<Announcement> getAnnouncements() {
        return announcementRepository.findAll();
    }

    @PostMapping("/announcements")
    public Announcement createAnnouncement(@RequestBody Announcement announcement) {
        return announcementRepository.save(announcement);
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> getLogs() {
        // CORREÇÃO: Chamando o método com o nome novo correto
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }
}