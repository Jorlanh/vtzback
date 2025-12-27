package com.votzz.backend.service;

import com.votzz.backend.domain.Vote;
import com.votzz.backend.repository.VoteRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final VoteRepository voteRepository;

    public ReportService(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    public ByteArrayInputStream generateAuditCsv(UUID assemblyId) {
        List<Vote> votes = voteRepository.findByAssemblyId(assemblyId);

        // Configuração do CSV
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("NOME", "CPF", "OPÇÃO", "HASH AUDITORIA", "DATA/HORA")
                .build();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(out);
             CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {

            for (Vote vote : votes) {
                csvPrinter.printRecord(
                    vote.getUser().getNome(),
                    maskCpf(vote.getUser().getCpf()),
                    vote.getOpcaoEscolhida(), 
                    vote.getAuditHash(),
                    // AGORA FUNCIONA: Herda de BaseEntity
                    vote.getCreatedAt() 
                );
            }

            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar CSV de auditoria: " + e.getMessage());
        }
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) return "***";
        if(cpf.contains(".")) {
             return "***." + cpf.substring(4, 7) + "." + cpf.substring(8, 11) + "-**";
        }
        return "***" + cpf.substring(3, 9) + "**";
    }
}