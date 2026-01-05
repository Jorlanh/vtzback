package com.votzz.backend.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminDashboardStats(
    long totalUsers,
    long onlineUsers,
    long activeAssemblies,
    BigDecimal engagement,
    long yearlyVotes,
    long attentionRequired,
    Map<String, Long> participationEvolution
) {
    // Construtor auxiliar para o AdminService (Votzz Global) não quebrar
    public AdminDashboardStats(long totalUsers, long onlineUsers, long totalTenants, long activeTenants, BigDecimal mrr, long attention) {
        this(totalUsers, onlineUsers, activeTenants, mrr, totalTenants, attention, null);
    }
}