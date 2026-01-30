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
    BigDecimal saldoAtual // [NOVO] Adicionado para o saldo aparecer
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
            BigDecimal.ZERO // Saldo padrão para Super Admin
        );
    }

    // --- CONSTRUTOR 2: Para o CondoDashboardService (Síndicos e Moradores) ---
    public AdminDashboardStats(long totalUsers, long onlineUsers, long activeAssemblies, BigDecimal engagement, long yearlyVotes, long attention, Map<String, Long> participationEvolution, BigDecimal saldoAtual) {
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
            saldoAtual // Recebe o saldo real do Service
        );
    }
}