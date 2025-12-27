package com.votzz.backend.dto;

import java.math.BigDecimal;

public record TopAfiliadoDTO(
    String nomeAfiliado,
    BigDecimal totalVendas
) {}