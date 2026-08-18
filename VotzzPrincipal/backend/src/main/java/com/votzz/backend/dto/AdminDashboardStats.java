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
    Map<String, Long> participationEvolution,
    BigDecimal saldoAtual,
    long pendingReceipts // [NOVO] Contador de comprovantes pendentes
) {
    // --- CONSTRUTOR 1: Para o AdminService (Super Admin) ---
    public AdminDashboardStats(long totalUsers, long onlineUsers, long totalTenants, long activeTenants, BigDecimal mrr) {
        this(
            totalUsers, 
            onlineUsers, 
            totalTenants, 
            activeTenants, 
            0,                  
            BigDecimal.ZERO,    
            mrr, 
            0,                  
            0,                  
            null,
            BigDecimal.ZERO,
            0 // pendingReceipts padrão para Super Admin
        );
    }

    // --- CONSTRUTOR 2: Para o CondoDashboardService (Síndicos e Moradores) ---
    public AdminDashboardStats(long totalUsers, long onlineUsers, long activeAssemblies, BigDecimal engagement, long yearlyVotes, long attention, Map<String, Long> participationEvolution, BigDecimal saldoAtual, long pendingReceipts) {
        this(
            totalUsers, 
            onlineUsers, 
            0,                  
            0,                  
            activeAssemblies, 
            engagement, 
            BigDecimal.ZERO,    
            yearlyVotes, 
            attention, 
            participationEvolution,
            saldoAtual,
            pendingReceipts // Recebe o valor real do Service
        );
    }
}