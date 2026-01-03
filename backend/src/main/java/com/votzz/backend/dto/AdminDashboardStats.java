package com.votzz.backend.dto;

import java.math.BigDecimal;

public record AdminDashboardStats(
    long totalUsers,
    long onlineUsers,
    long totalTenants,
    long activeTenants,
    BigDecimal mrr, // Mudado para BigDecimal para precisão financeira
    long latencyMs 
) {}