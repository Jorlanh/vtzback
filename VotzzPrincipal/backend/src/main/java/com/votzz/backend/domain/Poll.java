package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "polls") // Nome exato da tabela no SQL
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @JsonIgnore
    private Tenant tenant;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    private String status; 
    private String targetAudience; 

    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private UUID createdBy;

    // Campos novos que adicionamos no banco
    private LocalDateTime autoArchiveDate;
    private Boolean isArchived = false;

    // Relacionamento com as opções (tabela poll_options)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "poll_id") 
    private List<PollOption> options = new ArrayList<>();

    // Relacionamento com os votos (tabela poll_votes)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "poll_id")
    private List<PollVote> votes = new ArrayList<>();

    @Transient
    private boolean userHasVoted; 

    @PrePersist
    void onCreate() { 
        createdAt = LocalDateTime.now(); 
        if (status == null) status = "OPEN";
        if (isArchived == null) isArchived = false;
    }
}