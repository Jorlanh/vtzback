package com.votzz.backend.dto; // <--- Corrige o erro "declared package"

import lombok.Data;
import java.util.List; // <--- Corrige o erro "List cannot be resolved"
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class PollDTO {
    
    private UUID id; // <--- Alterado de Long para UUID para bater com sua Entidade
    private String title;
    private String description;
    
    private List<PollOptionDTO> options; // <--- Agora vai funcionar pois criamos a classe acima
    
    private boolean userHasVoted; // <--- O CAMPO MÁGICO
    private boolean isArchived;
    
    private String status;
    private LocalDateTime endDate;
}