package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "financial_reports")
public class FinancialReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String month; // Ex: "Janeiro"

    @Column(nullable = false)
    private Integer year; // Ex: 2026

    @Column(nullable = false)
    private String url; // URL do PDF (S3 ou local)

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}