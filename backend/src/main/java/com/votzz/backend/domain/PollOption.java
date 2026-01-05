package com.votzz.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "poll_options_data")
public class PollOption {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID) 
    private UUID id;
    
    private String label;
}