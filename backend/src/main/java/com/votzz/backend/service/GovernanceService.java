package com.votzz.backend.service;

import com.votzz.backend.domain.*;
import com.votzz.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GovernanceService {

    private final AssemblyRepository assemblyRepository;
    private final BookingRepository bookingRepository;
    private final AnnouncementRepository announcementRepository;
    private final PollRepository pollRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditLogRepository auditLogRepository;

    // --- DASHBOARD AGREGADO ---
    
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(User user) {
        UUID tenantId = user.getTenant().getId();
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Buscas
        List<Assembly> assemblies = assemblyRepository.findByTenantId(tenantId);
        List<Booking> bookings = bookingRepository.findAllByTenantId(tenantId);
        List<Announcement> allAnnouncements = announcementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<Poll> allPolls = pollRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<CalendarEvent> manualEvents = calendarEventRepository.findByTenantId(tenantId);

        // 2. Separação: Ativos vs Arquivados
        
        // Enquetes
        List<Poll> activePolls = new ArrayList<>();
        List<Poll> archivedPolls = new ArrayList<>();
        for (Poll p : allPolls) {
            if (p.getEndDate() != null && p.getEndDate().isBefore(now)) {
                p.setStatus("CLOSED"); 
                archivedPolls.add(p);
            } else {
                activePolls.add(p);
            }
        }

        // Comunicados (Arquiva automaticamente após 30 dias)
        List<Announcement> activeAnn = new ArrayList<>();
        List<Announcement> archivedAnn = new ArrayList<>();
        for (Announcement a : allAnnouncements) {
            if (a.getCreatedAt() != null && a.getCreatedAt().plusDays(30).isBefore(now)) {
                archivedAnn.add(a);
            } else {
                activeAnn.add(a);
            }
        }

        // 3. KPIs - Cálculo de Participação
        long totalUsers = userRepository.countByTenantId(tenantId); 
        if (totalUsers == 0) totalUsers = 1; 

        double participationRate = 0.0;
        if (!allPolls.isEmpty()) {
            long totalVotes = allPolls.stream().mapToLong(p -> p.getVotes().size()).sum();
            participationRate = ((double) totalVotes / (allPolls.size() * totalUsers)) * 100;
        }
        
        long unreadComms = activeAnn.stream().filter(a -> {
            if (a.getReadBy() == null) return true;
            return !a.getReadBy().contains(user.getId());
        }).count();
        
        // 4. Montagem da Timeline e Calendário
        List<Map<String, Object>> timeline = new ArrayList<>();
        List<Map<String, Object>> calendar = new ArrayList<>();

        activePolls.forEach(p -> timeline.add(item("POLL", "Nova Enquete: " + p.getTitle(), p.getCreatedAt(), "Síndico")));
        activeAnn.forEach(a -> timeline.add(item("COMMUNICATION", "Comunicado: " + a.getTitle(), a.getCreatedAt(), "Administração")));
        assemblies.forEach(a -> timeline.add(item("ASSEMBLY", "Assembleia: " + a.getTitulo(), a.getCreatedAt(), "Síndico")));
        
        timeline.sort((a, b) -> {
            LocalDateTime dateA = (LocalDateTime) a.get("date");
            LocalDateTime dateB = (LocalDateTime) b.get("date");
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA);
        });

        // Calendário Unificado
        bookings.stream().filter(b -> !"CANCELLED".equals(b.getStatus())).forEach(b -> 
            calendar.add(calendarItem(b.getBookingDate(), "Reserva: " + b.getCommonArea().getName(), "BOOKING", b.getId().toString()))
        );
        activePolls.forEach(p -> 
            calendar.add(calendarItem(p.getEndDate().toLocalDate(), "Fim Enquete: " + p.getTitle(), "POLL", p.getId().toString()))
        );
        manualEvents.forEach(e -> calendar.add(calendarItem(e.getDate(), e.getTitle(), e.getType(), e.getId().toString())));

        return Map.of(
            "kpis", Map.of(
                "activePolls", activePolls.size(), 
                "unreadComms", unreadComms, 
                "totalActions", timeline.size(),
                "participationRate", (int) participationRate
            ),
            "timeline", timeline.stream().limit(20).collect(Collectors.toList()),
            "calendar", calendar,
            "polls", Map.of("active", activePolls, "archived", archivedPolls),
            "announcements", Map.of("active", activeAnn, "archived", archivedAnn)
        );
    }

    // --- CRIAÇÃO (CREATE) ---

    @Transactional
    public void createAnnouncement(Announcement ann, User creator) {
        ann.setTenant(creator.getTenant());
        if(ann.getCreatedAt() == null) ann.setCreatedAt(LocalDateTime.now()); // Garante data
        announcementRepository.save(ann);
        logAction(creator, "CRIAR_COMUNICADO", "Novo comunicado: " + ann.getTitle());
        notifyAllUsers(creator.getTenant().getId(), "Novo Comunicado", "Aviso: " + ann.getTitle());
    }

    @Transactional
    public void createPoll(Poll poll, User creator) {
        poll.setTenant(creator.getTenant());
        pollRepository.save(poll);
        logAction(creator, "CRIAR_ENQUETE", "Nova enquete: " + poll.getTitle());
        notifyAllUsers(creator.getTenant().getId(), "Nova Enquete", "Participe: " + poll.getTitle());
    }
    
    @Transactional
    public void createCalendarEvent(CalendarEvent evt, User creator) {
        evt.setTenant(creator.getTenant());
        calendarEventRepository.save(evt);
        logAction(creator, "CRIAR_EVENTO", "Evento agendado: " + evt.getTitle());
    }

    // --- EDIÇÃO (UPDATE) ---

    @Transactional
    public void updateAnnouncement(UUID id, Announcement newData, User user) {
        announcementRepository.findById(id).ifPresent(ann -> {
            ann.setTitle(newData.getTitle());
            ann.setContent(newData.getContent());
            ann.setPriority(newData.getPriority());
            ann.setRequiresConfirmation(newData.getRequiresConfirmation());
            announcementRepository.save(ann);
            logAction(user, "EDITAR_COMUNICADO", "Editou comunicado ID: " + id);
        });
    }

    @Transactional
    public void updatePoll(UUID id, Poll newData, User user) {
        pollRepository.findById(id).ifPresent(poll -> {
            poll.setTitle(newData.getTitle());
            poll.setDescription(newData.getDescription());
            poll.setEndDate(newData.getEndDate());
            pollRepository.save(poll);
            logAction(user, "EDITAR_ENQUETE", "Editou enquete ID: " + id);
        });
    }

    @Transactional
    public void updateCalendarEvent(UUID id, CalendarEvent newData, User user) {
        calendarEventRepository.findById(id).ifPresent(evt -> {
            evt.setTitle(newData.getTitle());
            evt.setDate(newData.getDate());
            evt.setType(newData.getType());
            calendarEventRepository.save(evt);
            logAction(user, "EDITAR_EVENTO", "Editou evento ID: " + id);
        });
    }

    // --- EXCLUSÃO (DELETE) ---

    @Transactional
    public void deleteAnnouncement(UUID id, User user) {
        announcementRepository.findById(id).ifPresent(ann -> {
            announcementRepository.delete(ann);
            logAction(user, "EXCLUIR_COMUNICADO", "Excluiu comunicado: " + ann.getTitle());
        });
    }

    @Transactional
    public void deletePoll(UUID id, User user) {
        pollRepository.findById(id).ifPresent(poll -> {
            pollRepository.delete(poll);
            logAction(user, "EXCLUIR_ENQUETE", "Excluiu enquete: " + poll.getTitle());
        });
    }

    @Transactional
    public void deleteCalendarEvent(UUID id, User user) {
        calendarEventRepository.findById(id).ifPresent(evt -> {
            calendarEventRepository.delete(evt);
            logAction(user, "EXCLUIR_EVENTO", "Excluiu evento: " + evt.getTitle());
        });
    }

    // --- VOTAÇÃO & LEITURA ---

    @Transactional
    public void votePoll(UUID pollId, User voter, UUID optionId) {
        pollRepository.findById(pollId).ifPresent(poll -> {
            if (poll.getEndDate() != null && poll.getEndDate().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Esta enquete já está encerrada.");
            }
            if(poll.getVotes() == null) poll.setVotes(new ArrayList<>());
            
            // Remove voto anterior se existir
            poll.getVotes().removeIf(v -> v.getUserId().equals(voter.getId()));
            
            PollVote vote = new PollVote();
            vote.setUserId(voter.getId());
            vote.setOptionId(optionId);
            poll.getVotes().add(vote);
            pollRepository.save(poll);
            
            logAction(voter, "VOTAR_ENQUETE", "Registrou voto na enquete: " + poll.getTitle());
        });
    }

    @Transactional
    public void markAnnouncementAsRead(UUID annId, UUID userId) {
        announcementRepository.findById(annId).ifPresent(a -> {
            if(a.getReadBy() == null) a.setReadBy(new HashSet<>());
            a.getReadBy().add(userId);
            announcementRepository.save(a);
        });
    }

    // --- RELATÓRIOS (PDF GENERATOR) ---

    public ByteArrayInputStream generatePollReportPdf(UUID pollId, User requester) {
        Poll poll = pollRepository.findById(pollId).orElseThrow();
        StringBuilder txt = new StringBuilder();
        txt.append("=== RELATÓRIO DE ENQUETE - VOTZZ ===\n\n");
        txt.append("Título: ").append(poll.getTitle()).append("\n");
        txt.append("Status: ").append(poll.getStatus()).append("\n\n");
        txt.append("RESULTADOS:\n");
        
        for (PollOption opt : poll.getOptions()) {
            long count = poll.getVotes().stream().filter(v -> v.getOptionId().equals(opt.getId())).count();
            txt.append("- ").append(opt.getLabel()).append(": ").append(count).append(" votos\n");
        }
        txt.append("\nTotal de votos: ").append(poll.getVotes().size());
        
        logAction(requester, "DOWNLOAD_RELATORIO", "Baixou PDF da enquete: " + poll.getTitle());
        return new ByteArrayInputStream(txt.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    public Poll getPollById(UUID id) { return pollRepository.findById(id).orElseThrow(); }

    // --- HELPERS ---

    private void logAction(User user, String action, String details) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(LocalDateTime.now().toString());
            log.setAction(action);
            log.setDetails(details);
            log.setUserId(user.getId().toString());
            log.setUserName(user.getNome());
            log.setTenant(user.getTenant());
            log.setResourceType("GOVERNANCA");
            log.setIpAddress("APP_WEB"); 
            auditLogRepository.save(log);
        } catch (Exception e) { System.err.println("Erro auditoria: " + e.getMessage()); }
    }

    private void notifyAllUsers(UUID tenantId, String subject, String body) {
        try {
            List<String> emails = userRepository.findByTenantId(tenantId)
                    .stream().map(User::getEmail).collect(Collectors.toList());
            if (!emails.isEmpty()) emailService.sendGenericNotification(emails, subject, body); 
        } catch (Exception e) { System.err.println("Erro email: " + e.getMessage()); }
    }

    private Map<String, Object> item(String type, String desc, LocalDateTime date, String user) {
        return Map.of("type", type, "description", desc, "date", date != null ? date : LocalDateTime.now(), "user", user != null ? user : "Sistema");
    }
    
    private Map<String, Object> calendarItem(LocalDate date, String title, String type, String id) {
        return Map.of("id", id, "date", date, "title", title, "type", type);
    }
}