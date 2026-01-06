package com.votzz.backend.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminDashboardStats(
    long totalUsers,
    long onlineUsers,
    long totalTenants,
    long activeTenants,
    long activeAssemblies,
    BigDecimal engagement,
    BigDecimal mrr,
    long yearlyVotes,
    long attentionRequired,
    Map<String, Long> participationEvolution
) {
    // --- CONSTRUTOR 1: Para o AdminService (Super Admin / God Mode) ---
    // Recebe 5 argumentos. Preenche dados de condomínio local com 0/null.
    public AdminDashboardStats(long totalUsers, long onlineUsers, long totalTenants, long activeTenants, BigDecimal mrr) {
        this(
            totalUsers, 
            onlineUsers, 
            totalTenants, 
            activeTenants, 
            0,                  // activeAssemblies
            BigDecimal.ZERO,    // engagement
            mrr, 
            0,                  // yearlyVotes
            0,                  // attentionRequired
            null                // participationEvolution
        );
    }

    // --- CONSTRUTOR 2: Para o CondoDashboardService (Painel do Síndico) ---
    // Recebe 7 argumentos. Preenche dados globais (Tenants/MRR) com 0/Zero.
    public AdminDashboardStats(long totalUsers, long onlineUsers, long activeAssemblies, BigDecimal engagement, long yearlyVotes, long attention, Map<String, Long> participationEvolution) {
        this(
            totalUsers, 
            onlineUsers, 
            0,                  // totalTenants
            0,                  // activeTenants
            activeAssemblies, 
            engagement, 
            BigDecimal.ZERO,    // mrr (Condomínio não vê MRR global)
            yearlyVotes, 
            attention, 
            participationEvolution
        );
    }
}