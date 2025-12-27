package com.votzz.backend.dto;

import com.votzz.backend.domain.Tenant;
import java.math.BigDecimal;
import java.util.List;

public record AdminOverviewDTO(
    long totalCondominios,
    BigDecimal mrr,
    List<Tenant> condominiosVencendo,
    List<TopAfiliadoDTO> topAfiliados
) {}