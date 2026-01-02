package com.votzz.backend.dto;

public record AdminDashboardStats(
    long totalUsers,
    long onlineUsers,
    long totalTenants,
    long activeTenants,
    double mrr,
    long latencyMs 
) {}