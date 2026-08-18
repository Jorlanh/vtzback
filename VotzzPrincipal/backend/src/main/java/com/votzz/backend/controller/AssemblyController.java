package com.votzz.backend.controller;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.votzz.backend.domain.*;
import com.votzz.backend.repository.*;
import com.votzz.backend.service.AuditService;
import com.votzz.backend.service.EmailService;
import com.votzz.backend.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assemblies")
@RequiredArgsConstructor
public class AssemblyController {

    private static final Logger logger = LoggerFactory.getLogger(AssemblyController.class);

    private final AssemblyRepository assemblyRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    
    // Repositórios para o Dossiê
    private final ChatMessageRepository chatMessageRepository;
    private final AuditLogRepository auditLogRepository; 

    // --- ENDPOINTS PADRÃO ---

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal User currentUser) {
        try {
            if (currentUser == null) return ResponseEntity.status(401).body("Não autorizado");
            if (currentUser.getRole() != null && currentUser.getRole().name().equals("ADMIN")) {
                return ResponseEntity.ok(assemblyRepository.findAll());
            }
            UUID tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null && currentUser.getTenant() != null) tenantId = currentUser.getTenant().getId();
            if (tenantId == null) return ResponseEntity.badRequest().body(Map.of("error", "Condomínio não identificado."));
            return ResponseEntity.ok(assemblyRepository.findByTenantId(tenantId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro interno: " + e.getMessage());
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> criarAssembleia(@RequestBody Assembly assembly, @AuthenticationPrincipal User currentUser) {
        try {
            if (currentUser == null) return ResponseEntity.status(401).body("Usuário não autenticado.");
            UUID tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null && currentUser.getTenant() != null) tenantId = currentUser.getTenant().getId();
            if (tenantId == null) return ResponseEntity.badRequest().body("Erro de Tenant ID.");

            Tenant targetTenant = new Tenant();
            targetTenant.setId(tenantId);
            assembly.setTenant(targetTenant);

            if (assembly.getOptions() != null) {
                assembly.getOptions().forEach(o -> {
                    o.setTenant(targetTenant);
                    o.setAssembly(assembly);
                });
            }

            // Lógica de Link de Vídeo Padrão
            if ((assembly.getLinkVideoConferencia() == null || assembly.getLinkVideoConferencia().isEmpty()) &&
                (assembly.getYoutubeLiveUrl() == null || assembly.getYoutubeLiveUrl().isEmpty())) {
                assembly.setLinkVideoConferencia("https://meet.jit.si/votzz-" + UUID.randomUUID().toString().substring(0, 8));
            }

            if (assembly.getStatus() == null) assembly.setStatus("AGENDADA");
            if (assembly.getDataInicio() == null) assembly.setDataInicio(LocalDateTime.now());
            if (assembly.getDataFim() == null) assembly.setDataFim(LocalDateTime.now().plusDays(2));

            Assembly saved = assemblyRepository.save(assembly);
            auditService.log(currentUser, targetTenant, "CRIAR_ASSEMBLEIA", "Criou a assembleia: " + saved.getTitulo(), "ASSEMBLEIA");

            // Notificação Automática (Try-Catch para não bloquear criação se falhar email)
            try {
                List<User> residents = userRepository.findByTenantId(tenantId);
                List<String> emails = residents.stream()
                        .map(User::getEmail)
                        .filter(email -> email != null && !email.isEmpty())
                        .collect(Collectors.toList());

                if (!emails.isEmpty()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String inicio = saved.getDataInicio().format(formatter);
                    String fim = saved.getDataFim() != null ? saved.getDataFim().format(formatter) : "Indefinido";
                    String periodo = "De " + inicio + " até " + fim;
                    String link = "https://www.votzz.com.br/#/voting-room/" + saved.getId();
                    String tName = currentUser.getTenant() != null ? currentUser.getTenant().getNome() : "Condomínio";
                    emailService.sendNewAssemblyNotification(emails, saved.getTitulo(), saved.getDescription(), periodo, link, tName);
                }
            } catch (Exception e) {
                logger.error("Falha ao processar notificações automáticas: {}", e.getMessage());
            }
            
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assembly> getById(@PathVariable UUID id) {
        return assemblyRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> atualizarAssembleia(@PathVariable UUID id, @RequestBody Assembly u, @AuthenticationPrincipal User user) {
        return assemblyRepository.findById(id).map(a -> {
            a.setTitulo(u.getTitulo());
            a.setDescription(u.getDescription());
            a.setDataInicio(u.getDataInicio());
            a.setDataFim(u.getDataFim());
            a.setStatus(u.getStatus());
            a.setYoutubeLiveUrl(u.getYoutubeLiveUrl());
            a.setLinkVideoConferencia(u.getLinkVideoConferencia());
            Assembly saved = assemblyRepository.save(a);
            auditService.log(user, a.getTenant(), "EDITAR_ASSEMBLEIA", "Editou: " + saved.getTitulo(), "ASSEMBLEIA");
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> excluirAssembleia(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
         return assemblyRepository.findById(id).map(a -> {
             assemblyRepository.delete(a);
             auditService.log(currentUser, a.getTenant(), "EXCLUIR_ASSEMBLEIA", "Excluiu: " + a.getTitulo(), "ASSEMBLEIA");
             return ResponseEntity.ok(Map.of("message", "Excluída com sucesso."));
         }).orElse(ResponseEntity.notFound().build());
    }

    // ==================================================================================
    // GERAÇÃO DO DOSSIÊ JURÍDICO EM PDF (FINAL COM LOGO E LINK)
    // ==================================================================================
    @GetMapping("/{id}/dossier")
    public ResponseEntity<byte[]> exportarDossie(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        try {
            Assembly assembly = assemblyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Assembleia não encontrada"));

            if (currentUser == null || assembly.getTenant() == null || 
                !currentUser.getTenant().getId().equals(assembly.getTenant().getId())) {
                return ResponseEntity.status(403).body("Acesso negado.".getBytes());
            }

            // --- 1. COLETA DE DADOS ---
            List<Vote> votes = voteRepository.findByAssemblyId(id);
            List<ChatMessage> chatMessages = chatMessageRepository.findByAssemblyIdOrderByCreatedAtAsc(id);
            
            // [NOVO] Cálculo da Apuração para o Resultado Consolidado
            Map<String, Long> apuracao = votes.stream()
                .collect(Collectors.groupingBy(Vote::getOptionId, Collectors.counting()));

            // Logs
            LocalDateTime startLog = assembly.getDataInicio().minusDays(5); 
            LocalDateTime endLog = LocalDateTime.now().isAfter(assembly.getDataFim()) ? LocalDateTime.now() : assembly.getDataFim().plusHours(4);
            
            List<AuditLog> allLogs = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(assembly.getTenant().getId());
            List<AuditLog> logs = allLogs.stream()
                .filter(log -> log.getCreatedAt() != null && 
                               !log.getCreatedAt().isBefore(startLog) && 
                               !log.getCreatedAt().isAfter(endLog))
                .collect(Collectors.toList());

            // --- 2. LÓGICA DE DADOS ---
            String statusFinal = assembly.getStatus();
            String encerramentoReal = "Ainda em aberto";
            
            if ("ENCERRADA".equalsIgnoreCase(statusFinal)) {
                AuditLog closingLog = logs.stream()
                    .filter(log -> log.getAction().contains("ENCERRAR") || log.getAction().contains("CLOSE"))
                    .findFirst()
                    .orElse(null);

                if (closingLog != null) {
                    encerramentoReal = formatDate(closingLog.getCreatedAt()) + " (Manual por " + closingLog.getUserName() + ")";
                } else {
                    encerramentoReal = "Data não registrada no log (encerrado manualmente)";
                }
            } else if (assembly.getDataFim() != null && LocalDateTime.now().isAfter(assembly.getDataFim())) {
                encerramentoReal = "Expirada (Aguardando fechamento oficial)";
            }
            
            // Prioriza o YouTube, se não tiver, tenta o Link genérico (Jitsi/Meet)
            String linkTransmissao = null;
            if (assembly.getYoutubeLiveUrl() != null && !assembly.getYoutubeLiveUrl().isEmpty()) {
                linkTransmissao = assembly.getYoutubeLiveUrl();
            } else if (assembly.getLinkVideoConferencia() != null && !assembly.getLinkVideoConferencia().isEmpty()) {
                linkTransmissao = assembly.getLinkVideoConferencia();
            }

            // --- 3. CRIAÇÃO DO PDF ---
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(document, out);

            document.open();

            // Definição de Cores Votzz
            Color votzzGreen = new Color(16, 185, 129); // Emerald 500
            Color votzzDark  = new Color(15, 23, 42);   // Slate 900
            Color textGray   = new Color(51, 65, 85);   // Slate 700

            // Fontes
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, votzzDark);
            Font dotFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, votzzGreen); 
            Font iconFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, votzzDark); 
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textGray);
            Font linkFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLUE);
            Font smallFont = FontFactory.getFont(FontFactory.COURIER, 8, Color.DARK_GRAY);
            Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(21, 128, 61)); // Verde escuro

            // === CABEÇALHO COM LOGO SIMULADA ===
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 4});

            // Lado Esquerdo: Ícone da "Urna" (Quadrado Verde com "V")
            PdfPCell iconCell = new PdfPCell(new Phrase("V", iconFont));
            iconCell.setBackgroundColor(votzzGreen);
            iconCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            iconCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            iconCell.setBorder(Rectangle.NO_BORDER);
            iconCell.setPadding(8); 
            
            // Lado Direito: Texto da Marca
            Phrase brandPhrase = new Phrase();
            brandPhrase.add(new Chunk("Votzz", brandFont));
            brandPhrase.add(new Chunk(".", dotFont));
            brandPhrase.add(new Chunk("\nDecisões Inteligentes", FontFactory.getFont(FontFactory.HELVETICA, 8, votzzGreen)));
            
            PdfPCell brandCell = new PdfPCell(brandPhrase);
            brandCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.setPaddingLeft(10);

            headerTable.addCell(iconCell);
            headerTable.addCell(brandCell);
            
            // Tabela wrapper
            PdfPTable headerWrapper = new PdfPTable(1);
            headerWrapper.setWidthPercentage(30); 
            headerWrapper.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell wrapperCell = new PdfPCell(headerTable);
            wrapperCell.setBorder(Rectangle.NO_BORDER);
            headerWrapper.addCell(wrapperCell);
            
            document.add(headerWrapper);
            document.add(new Paragraph(" "));
            
            // Título do Documento
            Paragraph titleP = new Paragraph("DOSSIÊ JURÍDICO DIGITAL", titleFont);
            titleP.setAlignment(Element.ALIGN_CENTER);
            document.add(titleP);
            
            Paragraph subTitleP = new Paragraph("Auditoria Forense de Assembleia Virtual (Lei 14.010/20)", headerFont);
            subTitleP.setAlignment(Element.ALIGN_CENTER);
            document.add(subTitleP);
            document.add(new Paragraph(" "));

            // === RESUMO DA ASSEMBLEIA ===
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(10);
            infoTable.setWidths(new float[]{1.5f, 3.5f});
            
            addInfoRow(infoTable, "Condomínio:", assembly.getTenant().getNome(), textFont);
            addInfoRow(infoTable, "Pauta / Título:", assembly.getTitulo(), textFont);
            addInfoRow(infoTable, "ID do Registro:", assembly.getId().toString(), textFont);
            addInfoRow(infoTable, "Status Atual:", statusFinal, textFont);
            
            // LINK DA TRANSMISSÃO AGORA INCLUÍDO
            if (linkTransmissao != null) {
                addInfoRow(infoTable, "Link da Transmissão:", linkTransmissao, linkFont);
            } else {
                addInfoRow(infoTable, "Link da Transmissão:", "Não registrado", textFont);
            }
            
            addInfoRow(infoTable, "Início Agendado:", formatDate(assembly.getDataInicio()), textFont);
            addInfoRow(infoTable, "Fim Agendado:", formatDate(assembly.getDataFim()), textFont);
            
            // Exibe o encerramento real
            addInfoRow(infoTable, "Encerramento Efetivo:", encerramentoReal, 
                "ENCERRADA".equalsIgnoreCase(statusFinal) ? statusFont : textFont);
            
            addInfoRow(infoTable, "Gerado por:", currentUser.getNome() + " (CPF: " + currentUser.getCpf() + ")", textFont);
            
            document.add(infoTable);
            document.add(new Paragraph(" "));

            // =========================================================================
            // [NOVO] DESCRIÇÃO DA PAUTA / ORDEM DO DIA
            // =========================================================================
            addSectionHeader(document, "PAUTA / ORDEM DO DIA", headerFont);
            String descricaoTexto = assembly.getDescription() != null && !assembly.getDescription().isEmpty() 
                                    ? assembly.getDescription() 
                                    : "Nenhuma descrição detalhada foi fornecida para esta assembleia.";
            
            Paragraph pautaP = new Paragraph(descricaoTexto, textFont);
            pautaP.setSpacingAfter(10);
            pautaP.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(pautaP);
            document.add(new Paragraph(" "));

            // =========================================================================
            // [NOVO] RESULTADO CONSOLIDADO (MOVIDO PARA CIMA)
            // =========================================================================
            addSectionHeader(document, "RESULTADO CONSOLIDADO DA VOTAÇÃO", headerFont);
            document.add(new Paragraph("Totalização final dos votos válidos:", textFont));
            
            PdfPTable resultTable = new PdfPTable(2);
            resultTable.setWidthPercentage(100);
            resultTable.setWidths(new float[]{4, 1}); // Nome largo, número curto
            resultTable.setSpacingBefore(5);
            
            // Header manual para 2 colunas
            PdfPCell hOpcao = new PdfPCell(new Phrase("Opção", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
            hOpcao.setBackgroundColor(Color.GRAY);
            resultTable.addCell(hOpcao);
            
            PdfPCell hTotal = new PdfPCell(new Phrase("Total de Votos", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
            hTotal.setBackgroundColor(Color.GRAY);
            resultTable.addCell(hTotal);

            if (apuracao.isEmpty()) {
                PdfPCell c = new PdfPCell(new Phrase("Não houve votos computados.", textFont));
                c.setColspan(2);
                resultTable.addCell(c);
            } else {
                apuracao.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEach(entry -> {
                        resultTable.addCell(new Phrase(entry.getKey(), textFont));
                        resultTable.addCell(new Phrase(entry.getValue().toString(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, votzzDark)));
                    });
            }
            document.add(resultTable);
            document.add(new Paragraph(" "));

            // === 1. LOGS (CONVOCAÇÃO) ===
            addSectionHeader(document, "1. COMPROVANTE DE CONVOCAÇÃO (Logs de Sistema)", headerFont);
            document.add(new Paragraph("Evidências de disparo de notificações para os condôminos:", textFont));
            
            PdfPTable logsTable = new PdfPTable(3);
            logsTable.setWidthPercentage(100);
            logsTable.setWidths(new float[]{2.5f, 2.5f, 5});
            logsTable.setSpacingBefore(5);
            
            addTableHeader(logsTable, "Ação", "Data/Hora", "Detalhes", headerFont);
            
            boolean hasNotification = false;
            for (AuditLog log : logs) {
                if (log.getAction() != null && (log.getAction().contains("NOTIFICAR") || log.getAction().contains("CRIAR_ASSEMBLEIA"))) {
                    logsTable.addCell(new Phrase(log.getAction(), smallFont));
                    logsTable.addCell(new Phrase(formatDate(log.getCreatedAt()), smallFont));
                    logsTable.addCell(new Phrase(log.getDetails(), smallFont));
                    hasNotification = true;
                }
            }
            if(!hasNotification) {
               PdfPCell c = new PdfPCell(new Phrase("Nenhum log de disparo encontrado.", smallFont));
               c.setColspan(3);
               logsTable.addCell(c);
            }
            document.add(logsTable);
            document.add(new Paragraph(" "));

            // === 2. LISTA DE PRESENÇA ===
            addSectionHeader(document, "2. LISTA DE PRESENÇA DIGITAL (IP & Acessos)", headerFont);
            document.add(new Paragraph("Registro de atividades (Login, Votos, Chat) durante a sessão:", textFont));
            
            PdfPTable presenceTable = new PdfPTable(4);
            presenceTable.setWidthPercentage(100);
            presenceTable.setWidths(new float[]{3, 3, 2, 2});
            presenceTable.setSpacingBefore(5);
            addTableHeader(presenceTable, "Usuário", "Ação", "IP", "Data", headerFont);

            int activityCount = 0;
            for (AuditLog log : logs) {
                 if (log.getAction() != null && !log.getAction().contains("SCHEDULER")) {
                     presenceTable.addCell(new Phrase(log.getUserName() != null ? log.getUserName() : "Sistema", smallFont));
                     presenceTable.addCell(new Phrase(log.getAction(), smallFont));
                     presenceTable.addCell(new Phrase(log.getIpAddress() != null ? log.getIpAddress() : "-", smallFont));
                     presenceTable.addCell(new Phrase(formatDate(log.getCreatedAt()), smallFont));
                     activityCount++;
                 }
                 if(activityCount > 300) break; 
            }
            document.add(presenceTable);
            document.add(new Paragraph("Total de registros: " + activityCount, smallFont));
            document.add(new Paragraph(" "));

            // === 3. AUDITORIA DE VOTOS ===
            addSectionHeader(document, "3. AUDITORIA DE VOTOS (Registro Individual)", headerFont);
            document.add(new Paragraph("Registro imutável dos votos com hash criptográfico:", textFont));
            
            PdfPTable voteTable = new PdfPTable(4);
            voteTable.setWidthPercentage(100);
            voteTable.setWidths(new float[]{1.5f, 3, 2, 3.5f});
            voteTable.setSpacingBefore(5);
            addTableHeader(voteTable, "Unidade", "Usuário", "Opção", "Hash / Data", headerFont);

            if (votes.isEmpty()) {
                PdfPCell c = new PdfPCell(new Phrase("Nenhum voto registrado.", textFont));
                c.setColspan(4);
                voteTable.addCell(c);
            } else {
                for (Vote v : votes) {
                    voteTable.addCell(new Phrase(v.getUnidade(), smallFont));
                    voteTable.addCell(new Phrase(v.getUser() != null ? v.getUser().getNome() : "Excluído", smallFont));
                    voteTable.addCell(new Phrase(v.getOptionId(), smallFont));
                    String hashInfo = (v.getHash() != null ? v.getHash().substring(0, 16) + "..." : "S/H") + "\n" + formatDate(v.getCreatedAt());
                    voteTable.addCell(new Phrase(hashInfo, smallFont));
                }
            }
            document.add(voteTable);
            document.add(new Paragraph(" "));

            // === 5. CHAT ===
            addSectionHeader(document, "5. HISTÓRICO DE DELIBERAÇÃO (Chat)", headerFont);
            document.add(new Paragraph("Registro de debates ocorridos durante a sessão virtual:", textFont));
            document.add(new Paragraph(" "));

            for (ChatMessage msg : chatMessages) {
                String time = msg.getCreatedAt() != null ? msg.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "";
                String line = String.format("[%s] %s: %s", time, msg.getSenderName(), msg.getContent());
                document.add(new Paragraph(line, smallFont));
            }
            if(chatMessages.isEmpty()) document.add(new Paragraph("(Sem mensagens)", smallFont));

            // === RODAPÉ E ASSINATURA ===
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            
            // Linha de assinatura centralizada
            PdfPTable signatureTable = new PdfPTable(1);
            signatureTable.setWidthPercentage(60);
            signatureTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell sigCell = new PdfPCell();
            sigCell.setBorder(Rectangle.TOP); 
            sigCell.setBorderWidthTop(1f);
            sigCell.setBorderColorTop(Color.BLACK);
            
            Paragraph sigP = new Paragraph("Assinado digitalmente pelo Presidente da Mesa / Síndico", textFont);
            sigP.setAlignment(Element.ALIGN_CENTER);
            sigCell.addElement(sigP);
            signatureTable.addCell(sigCell);
            
            document.add(signatureTable);
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            Paragraph hashLine = new Paragraph("Hash de Integridade do Arquivo: " + UUID.randomUUID(), smallFont);
            hashLine.setAlignment(Element.ALIGN_CENTER);
            document.add(hashLine);
            
            Paragraph systemLine = new Paragraph("Gerado pelo sistema Votzz - Tecnologia para Assembleias Virtuais.", smallFont);
            systemLine.setAlignment(Element.ALIGN_CENTER);
            document.add(systemLine);

            document.close();

            // Nome do arquivo
            String safeTenantName = removeAccents(assembly.getTenant().getNome()).replaceAll("[^a-zA-Z0-9]", "_");
            String safeTitle = removeAccents(assembly.getTitulo()).replaceAll("[^a-zA-Z0-9]", "_");
            String filename = String.format("%s_%s_%s.pdf", safeTenantName, safeTitle, assembly.getId().toString().substring(0, 8));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());

        } catch (Exception e) {
            logger.error("Erro ao gerar dossiê PDF: ", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // --- Helpers Privados ---

    private String removeAccents(String str) {
        if (str == null) return "";
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "-", font));
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c2);
    }
    
    private void addInfoRow(PdfPTable table, String label, String value, Font font, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c2);
    }

    private void addSectionHeader(Document doc, String title, Font font) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setBackgroundColor(new Color(240, 240, 240)); 
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOTTOM);
        table.addCell(cell);
        doc.add(table);
    }

    private void addTableHeader(PdfPTable table, String h1, String h2, String h3, String h4, Font font) {
        Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        for(String h : new String[]{h1, h2, h3, h4}) {
            PdfPCell c = new PdfPCell(new Phrase(h, white));
            c.setBackgroundColor(Color.GRAY);
            table.addCell(c);
        }
    }
    private void addTableHeader(PdfPTable table, String h1, String h2, String h3, Font font) {
        Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        for(String h : new String[]{h1, h2, h3}) {
            PdfPCell c = new PdfPCell(new Phrase(h, white));
            c.setBackgroundColor(Color.GRAY);
            table.addCell(c);
        }
    }

    private String formatDate(LocalDateTime dt) {
        if(dt == null) return "-";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    private String formatTime(LocalDateTime dt) {
        if(dt == null) return "-";
        return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // --- MÉTODOS DE SUPORTE (Votar, Notificar, Fechar) ---
    @PostMapping("/{id}/notify")
    public ResponseEntity<?> notificarMoradores(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        try {
            Assembly assembly = assemblyRepository.findById(id).orElseThrow(() -> new RuntimeException("404"));
            
            // Segurança básica
            List<User> residents = userRepository.findByTenantId(assembly.getTenant().getId());
            List<String> recipients = residents.stream().map(User::getEmail).filter(e -> e != null && e.contains("@")).collect(Collectors.toList());
            
            if (!recipients.isEmpty()) {
                String link = "https://www.votzz.com.br/#/voting-room/" + assembly.getId();
                // Passa o nome do condomínio para o email
                String tName = assembly.getTenant() != null ? assembly.getTenant().getNome() : "Seu Condomínio";
                
                emailService.sendNewAssemblyNotification(recipients, assembly.getTitulo(), assembly.getDescription(), "Ver detalhes", link, tName);
                
                auditService.log(currentUser, assembly.getTenant(), "NOTIFICAR_ASSEMBLEIA", "Disparou notificações.", "ASSEMBLEIA");
            }
            return ResponseEntity.ok(Map.of("message", "Notificações enviadas."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/vote")
    @Transactional
    public ResponseEntity<?> votar(@PathVariable UUID id, @RequestBody VoteRequest request, @AuthenticationPrincipal User currentUser) {
        var assemblyOpt = assemblyRepository.findById(id);
        if (assemblyOpt.isEmpty()) return ResponseEntity.notFound().build();
        Assembly assembly = assemblyOpt.get();

        if ("ENCERRADA".equalsIgnoreCase(assembly.getStatus())) return ResponseEntity.badRequest().body("Encerrada");

        User voter = userRepository.findById(request.userId() != null ? request.userId() : currentUser.getId()).orElse(currentUser);
        List<String> units = request.units() != null ? request.units() : List.of("Unidade Padrão");
        
        StringBuilder receipt = new StringBuilder();
        for(String u : units) {
            if(!voteRepository.existsByAssemblyIdAndUnidade(id, u)) {
                Vote v = new Vote();
                v.setAssembly(assembly);
                v.setUser(voter);
                v.setTenant(assembly.getTenant());
                v.setOptionId(request.optionId());
                v.setUnidade(u);
                v.setHash(UUID.randomUUID().toString());
                v.setFraction(BigDecimal.ONE);
                voteRepository.save(v);
                receipt.append(v.getHash().substring(0,8)).append(";");
            }
        }
        auditService.log(voter, assembly.getTenant(), "VOTO_REGISTRADO", "Votou na assembleia", "VOTACAO");
        return ResponseEntity.ok(Map.of("id", receipt.toString(), "message", "Voto computado"));
    }

    @PatchMapping("/{id}/close") 
    public ResponseEntity<?> encerrar(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return assemblyRepository.findById(id).map(assembly -> {
            assembly.setStatus("ENCERRADA"); 
            assemblyRepository.save(assembly);
            auditService.log(currentUser, assembly.getTenant(), "ENCERRAR_ASSEMBLEIA", "Encerrou a assembleia", "ASSEMBLEIA");
            return ResponseEntity.ok(Map.of("message", "Encerrada com sucesso."));
        }).orElse(ResponseEntity.notFound().build());
    }

    public record VoteRequest(String optionId, UUID userId, List<String> units) {}
}