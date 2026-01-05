package com.votzz.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore; // Importante!
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "polls")
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @JsonIgnore // <--- ADICIONE ISSO
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "poll_id") 
    private List<PollOption> options = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "poll_id")
    private List<PollVote> votes = new ArrayList<>();

    @PrePersist
    void onCreate() { 
        createdAt = LocalDateTime.now(); 
        if (status == null) status = "OPEN";
    }
}