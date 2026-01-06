package com.votzz.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.votzz.backend.domain.*;
import com.votzz.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List; // Garante que é java.util.List
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
        
        List<Assembly> assemblies = assemblyRepository.findByTenantId(tenantId);
        List<Booking> bookings = bookingRepository.findAllByTenantId(tenantId);
        List<Announcement> allAnnouncements = announcementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<Poll> allPolls = pollRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<CalendarEvent> manualEvents = calendarEventRepository.findByTenantId(tenantId);

        // Processamento de Enquetes
        List<Poll> activePolls = new ArrayList<>();
        List<Poll> archivedPolls = new ArrayList<>();
        
        for (Poll p : allPolls) {
            // Verifica voto
            boolean hasVoted = false;
            if (p.getVotes() != null) {
                hasVoted = p.getVotes().stream()
                    .anyMatch(v -> v.getUserId().equals(user.getId()));
            }
            p.setUserHasVoted(hasVoted);

            // Verifica Arquivamento
            boolean isExpired = (p.getEndDate() != null && p.getEndDate().isBefore(now)) || 
                                (p.getAutoArchiveDate() != null && p.getAutoArchiveDate().isBefore(now));
                                
            if (Boolean.TRUE.equals(p.getIsArchived()) || isExpired) {
                if(!"CLOSED".equals(p.getStatus())) { 
                    p.setStatus("CLOSED"); 
                }
                archivedPolls.add(p);
            } else {
                activePolls.add(p);
            }
        }

        // Processamento de Comunicados
        List<Announcement> activeAnn = new ArrayList<>();
        List<Announcement> archivedAnn = new ArrayList<>();
        
        for (Announcement a : allAnnouncements) {
            // Verifica leitura (IMPORTANTE: Apenas verifica se o ID está no Set)
            boolean leu = a.getReadBy() != null && a.getReadBy().contains(user.getId());
            a.setReadByCurrentUser(leu);

            // Verifica Arquivamento
            boolean isExpired = (a.getAutoArchiveDate() != null && a.getAutoArchiveDate().isBefore(now));
            
            if (Boolean.TRUE.equals(a.getIsArchived()) || isExpired) {
                archivedAnn.add(a);
            } else {
                activeAnn.add(a);
            }
        }

        // KPIs
        long totalUsers = userRepository.countByTenantId(tenantId); 
        if (totalUsers == 0) totalUsers = 1; 

        double participationRate = 0.0;
        if (!allPolls.isEmpty()) {
            long totalVotes = allPolls.stream()
                .filter(p -> p.getVotes() != null)
                .mapToLong(p -> p.getVotes().size())
                .sum();
            participationRate = ((double) totalVotes / (allPolls.size() * totalUsers)) * 100;
        }
        
        long unreadComms = activeAnn.stream().filter(a -> !a.isReadByCurrentUser()).count();
        
        // Timeline & Calendar
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

        bookings.stream().filter(b -> !"CANCELLED".equals(b.getStatus())).forEach(b -> 
            calendar.add(calendarItem(b.getBookingDate(), "Reserva: " + b.getCommonArea().getName(), "BOOKING", b.getId().toString()))
        );
        activePolls.stream().filter(p -> p.getEndDate() != null).forEach(p -> 
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

    // --- ARQUIVAMENTO AUTOMÁTICO ---
    @Scheduled(cron = "0 0 * * * *") 
    @Transactional
    public void runAutoArchiving() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Announcement> allA = announcementRepository.findAll();
        for(Announcement a : allA) {
            if(!Boolean.TRUE.equals(a.getIsArchived()) && a.getAutoArchiveDate() != null && a.getAutoArchiveDate().isBefore(now)) {
                a.setIsArchived(true);
                announcementRepository.save(a);
                int leituras = (a.getReadBy() != null) ? a.getReadBy().size() : 0;
                logSystemAction(a.getTenant(), "ARQUIVAR_COMUNICADO_AUTO", 
                    "Comunicado '" + a.getTitle() + "' arquivado auto. Leituras: " + leituras);
            }
        }

        List<Poll> allP = pollRepository.findAll();
        for(Poll p : allP) {
             boolean shouldArchive = !Boolean.TRUE.equals(p.getIsArchived()) && 
                                     ((p.getAutoArchiveDate() != null && p.getAutoArchiveDate().isBefore(now)) || 
                                      (p.getEndDate() != null && p.getEndDate().isBefore(now)));
             if(shouldArchive) {
                p.setIsArchived(true);
                p.setStatus("CLOSED");
                pollRepository.save(p);
                int votos = (p.getVotes() != null) ? p.getVotes().size() : 0;
                logSystemAction(p.getTenant(), "ARQUIVAR_ENQUETE_AUTO", 
                    "Enquete '" + p.getTitle() + "' encerrada auto. Votos: " + votos);
            }
        }
    }

    // --- CRIAÇÃO & EDIÇÃO ---
    @Transactional
    public void createAnnouncement(Announcement ann, User creator) {
        ann.setTenant(creator.getTenant());
        if(ann.getCreatedAt() == null) ann.setCreatedAt(LocalDateTime.now());
        announcementRepository.save(ann);
        logAction(creator, "CRIAR_COMUNICADO", "Novo comunicado: " + ann.getTitle());
        notifyAllUsers(creator.getTenant().getId(), "Novo Comunicado", "Aviso: " + ann.getTitle());
    }
    
    @Transactional
    public void createPoll(Poll poll, User creator) {
        poll.setTenant(creator.getTenant());
        poll.setCreatedBy(creator.getId());
        pollRepository.save(poll);
        logAction(creator, "CRIAR_ENQUETE", "Nova enquete: " + poll.getTitle());
        notifyAllUsers(creator.getTenant().getId(), "Nova Enquete", "Participe: " + poll.getTitle());
    }

    @Transactional
    public void updateAnnouncement(UUID id, Announcement newData, User user) {
        announcementRepository.findById(id).ifPresent(ann -> {
            ann.setTitle(newData.getTitle());
            ann.setContent(newData.getContent());
            ann.setPriority(newData.getPriority());
            ann.setRequiresConfirmation(newData.getRequiresConfirmation());
            ann.setAutoArchiveDate(newData.getAutoArchiveDate());
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
            poll.setAutoArchiveDate(newData.getAutoArchiveDate());
            pollRepository.save(poll);
            logAction(user, "EDITAR_ENQUETE", "Editou enquete ID: " + id);
        });
    }
    
    @Transactional
    public void createCalendarEvent(CalendarEvent evt, User creator) {
        evt.setTenant(creator.getTenant());
        calendarEventRepository.save(evt);
        logAction(creator, "CRIAR_EVENTO", "Evento agendado: " + evt.getTitle());
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
            if ((poll.getEndDate() != null && poll.getEndDate().isBefore(LocalDateTime.now())) || "CLOSED".equals(poll.getStatus())) {
                throw new RuntimeException("Esta enquete já está encerrada.");
            }
            if(poll.getVotes() == null) poll.setVotes(new ArrayList<>());
            
            poll.getVotes().removeIf(v -> v.getUserId().equals(voter.getId()));
            
            PollVote vote = new PollVote();
            vote.setUserId(voter.getId());
            vote.setOptionId(optionId);
            poll.getVotes().add(vote);
            pollRepository.save(poll);
            
            logAction(voter, "VOTAR_ENQUETE", "Votou na enquete: " + poll.getTitle());
        });
    }

    // === CORREÇÃO DA LEITURA DE COMUNICADO ===
    // Garante que só adiciona se não existir, e SALVA no banco.
    @Transactional
    public void markAnnouncementAsRead(UUID annId, UUID userId) {
        Announcement ann = announcementRepository.findById(annId)
            .orElseThrow(() -> new RuntimeException("Comunicado não encontrado"));
            
        if(ann.getReadBy() == null) {
            ann.setReadBy(new HashSet<>());
        }
        
        // Só adiciona e salva se o usuário ainda NÃO leu
        if (!ann.getReadBy().contains(userId)) {
            ann.getReadBy().add(userId);
            announcementRepository.save(ann); // Commit no banco
        }
    }

    // === CORREÇÃO DA GERAÇÃO DE PDF (PDF REAL) ===
    public ByteArrayInputStream generatePollReportPdf(UUID pollId, User requester) {
        Poll poll = pollRepository.findById(pollId).orElseThrow();
        
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fontes
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font smallFont = FontFactory.getFont(FontFactory.COURIER, 10);

            // Cabeçalho
            Paragraph title = new Paragraph("DOSSIÊ DE AUDITORIA - VOTZZ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Dados da Enquete
            document.add(new Paragraph("DADOS DA MICRO-DECISÃO", boldFont));
            document.add(new Paragraph("Título: " + poll.getTitle(), normalFont));
            document.add(new Paragraph("Descrição: " + poll.getDescription(), normalFont));
            document.add(new Paragraph("Status: " + poll.getStatus(), normalFont));
            
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            document.add(new Paragraph("Criado em: " + poll.getCreatedAt().format(fmt), normalFont));
            
            if(poll.getEndDate() != null) {
                document.add(new Paragraph("Vence em: " + poll.getEndDate().format(fmt), normalFont));
            }
            
            document.add(new Paragraph("----------------------------------------------------------------", normalFont));
            document.add(new Paragraph(" "));

            // Resultados
            document.add(new Paragraph("RESULTADOS DA VOTAÇÃO", boldFont));
            int totalVotes = (poll.getVotes() != null) ? poll.getVotes().size() : 0;

            for (PollOption opt : poll.getOptions()) {
                long count = 0;
                if (poll.getVotes() != null) {
                    count = poll.getVotes().stream().filter(v -> v.getOptionId().equals(opt.getId())).count();
                }
                double percentage = (totalVotes > 0) ? (count * 100.0 / totalVotes) : 0;
                
                String line = String.format("- %s: %d votos (%.1f%%)", opt.getLabel(), count, percentage);
                document.add(new Paragraph(line, normalFont));
            }
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total de Votos: " + totalVotes, boldFont));
            
            // Rodapé de Auditoria
            document.add(new Paragraph(" "));
            document.add(new Paragraph("----------------------------------------------------------------", normalFont));
            document.add(new Paragraph("REGISTRO DE AUDITORIA", boldFont));
            document.add(new Paragraph("Gerado por: " + requester.getNome() + " (ID: " + requester.getId() + ")", smallFont));
            document.add(new Paragraph("Data: " + LocalDateTime.now().format(fmt), smallFont));
            document.add(new Paragraph("Hash de Integridade: " + UUID.randomUUID().toString(), smallFont));

            document.close();
            
            // Loga a ação de download
            logAction(requester, "DOWNLOAD_DOSSIE", "Baixou Dossiê PDF da enquete: " + poll.getTitle());

        } catch (DocumentException e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
    
    public Poll getPollById(UUID id) { return pollRepository.findById(id).orElseThrow(); }

    // --- HELPERS ---

    private void logAction(User user, String action, String details) {
        saveLog(user.getId().toString(), user.getNome(), user.getTenant(), action, details);
    }
    
    private void logSystemAction(Tenant tenant, String action, String details) {
        saveLog("SISTEMA", "Sistema Votzz", tenant, action, details);
    }

    private void saveLog(String userId, String userName, Tenant tenant, String action, String details) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(LocalDateTime.now().toString());
            log.setAction(action);
            log.setDetails(details);
            log.setUserId(userId);
            log.setUserName(userName);
            log.setTenant(tenant);
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