package com.votzz.backend.service;

import com.votzz.backend.dto.AdminDashboardStats;
import com.votzz.backend.repository.*;
import com.votzz.backend.domain.Assembly;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CondoDashboardService {

    private final UserRepository userRepository;
    private final AssemblyRepository assemblyRepository;
    private final VoteRepository voteRepository;

    public AdminDashboardStats getCondoStats(UUID tenantId) {
        // 1. Total de Moradores reais do condomínio
        long totalUsers = userRepository.findByTenantId(tenantId).size();
        
        // 2. Usuários Online (baseado no seu lastSeen do UserRepository)
        long onlineUsers = userRepository.countOnlineUsers(LocalDateTime.now().minusMinutes(5));

        // 3. Assembleias Ativas (Soma real de AGENDADA e ABERTA)
        long agendadas = assemblyRepository.countByTenantIdAndStatus(tenantId, "AGENDADA");
        long abertas = assemblyRepository.countByTenantIdAndStatus(tenantId, "ABERTA");
        long activeAssemblies = agendadas + abertas;

        // 4. Votos no Ano (Somente deste condomínio)
        LocalDateTime startOfYear = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0);
        long yearlyVotes = voteRepository.countByTenantIdAndCreatedAtAfter(tenantId, startOfYear);

        // 5. Cálculo de Engajamento Médio Real
        BigDecimal engagement = BigDecimal.ZERO;
        if (totalUsers > 0) {
            long totalVotesAllTime = voteRepository.countByTenantId(tenantId);
            engagement = BigDecimal.valueOf(totalVotesAllTime)
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(totalUsers), 0, RoundingMode.HALF_UP);
        }

        // 6. Atenção Necessária (Assembleias que vencem em menos de 48h)
        List<Assembly> activeList = assemblyRepository.findByTenantIdAndStatus(tenantId, "AGENDADA");
        activeList.addAll(assemblyRepository.findByTenantIdAndStatus(tenantId, "ABERTA"));
        
        long attention = activeList.stream()
                .filter(a -> a.getDataFim() != null && a.getDataFim().isBefore(LocalDateTime.now().plusDays(2)))
                .count();

        return new AdminDashboardStats(
            totalUsers,
            onlineUsers,
            activeAssemblies,
            engagement,
            yearlyVotes,
            attention,
            new HashMap<>()
        );
    }
}