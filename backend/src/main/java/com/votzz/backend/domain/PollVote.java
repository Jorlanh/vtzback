package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "poll_votes_data")
public class PollVote {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID) 
    private UUID id;
    
    private UUID userId;
    private UUID optionId;
}