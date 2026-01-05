package com.votzz.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PollOptionDTO {
    private UUID id;
    private String label;
}