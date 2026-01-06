package com.votzz.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.votzz.backend.domain.Poll;
import com.votzz.backend.domain.PollOption;
import com.votzz.backend.domain.PollVote;
import com.votzz.backend.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PollReportService {

    private final PollRepository pollRepository;

    public ByteArrayInputStream generatePollPdf(UUID pollId) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new RuntimeException("Decisão não encontrada."));

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fontes
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // Conteúdo
            Paragraph title = new Paragraph("AUDITORIA DE MICRO-DECISÃO", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("ID: " + poll.getId(), normalFont));
            document.add(new Paragraph("Título: " + poll.getTitle(), boldFont));
            document.add(new Paragraph("Descrição: " + poll.getDescription(), normalFont));
            document.add(new Paragraph("Status: " + poll.getStatus(), normalFont));
            document.add(new Paragraph("--------------------------------------------------"));
            
            document.add(new Paragraph("RESULTADO:", boldFont));
            List<PollVote> votes = poll.getVotes();
            int total = votes.size();

            for (PollOption opt : poll.getOptions()) {
                long count = votes.stream().filter(v -> v.getOptionId().equals(opt.getId())).count();
                double pct = total > 0 ? (count * 100.0 / total) : 0;
                document.add(new Paragraph(String.format("%s: %d votos (%.1f%%)", opt.getLabel(), count, pct), normalFont));
            }
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total de Participantes: " + total, boldFont));

            document.close();

        } catch (DocumentException e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}