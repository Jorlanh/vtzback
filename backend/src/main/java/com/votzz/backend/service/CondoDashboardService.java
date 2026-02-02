package com.votzz.backend.service;

import com.votzz.backend.domain.CondoFinancial;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CondoDashboardService {

    private final UserRepository userRepository;
    private final AssemblyRepository assemblyRepository;
    private final VoteRepository voteRepository;
    private final CondoFinancialRepository condoFinancialRepository;
    private final BookingRepository bookingRepository; // [1] INJEÇÃO DO REPOSITÓRIO DE RESERVAS

    public AdminDashboardStats getCondoStats(UUID tenantId) {
        // 1. Total de Moradores
        long totalUsers = userRepository.findByTenantId(tenantId).size();
        
        // 2. Usuários Online
        long onlineUsers = userRepository.countOnlineUsers(LocalDateTime.now().minusMinutes(5));

        // 3. Assembleias Ativas
        long agendadas = assemblyRepository.countByTenantIdAndStatus(tenantId, "AGENDADA");
        long abertas = assemblyRepository.countByTenantIdAndStatus(tenantId, "ABERTA");
        long activeAssemblies = agendadas + abertas;

        // 4. Votos no Ano
        LocalDateTime startOfYear = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0);
        long yearlyVotes = voteRepository.countByTenantIdAndCreatedAtAfter(tenantId, startOfYear);

        // 5. Engajamento
        BigDecimal engagement = BigDecimal.ZERO;
        if (totalUsers > 0) {
            long totalVotesAllTime = voteRepository.countByTenantId(tenantId);
            engagement = BigDecimal.valueOf(totalVotesAllTime)
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(totalUsers), 0, RoundingMode.HALF_UP);
        }

        // 6. Atenção Necessária
        List<Assembly> activeList = assemblyRepository.findByTenantIdAndStatus(tenantId, "AGENDADA");
        activeList.addAll(assemblyRepository.findByTenantIdAndStatus(tenantId, "ABERTA"));
        long attention = activeList.stream()
                .filter(a -> a.getDataFim() != null && a.getDataFim().isBefore(LocalDateTime.now().plusDays(2)))
                .count();

        // 7. Saldo Financeiro
        BigDecimal saldoAtual = BigDecimal.ZERO;
        Optional<CondoFinancial> financialOpt = condoFinancialRepository.findByTenantId(tenantId);
        
        if (financialOpt.isPresent()) {
            saldoAtual = financialOpt.get().getBalance(); 
        }

        // 8. [NOVO] Comprovantes Pendentes de Validação
        // Conta quantas reservas estão com status "UNDER_ANALYSIS" neste condomínio
        long pendingReceipts = bookingRepository.countByTenantIdAndStatus(tenantId, "UNDER_ANALYSIS");

        // Retorna o DTO completo
        return new AdminDashboardStats(
            totalUsers,
            onlineUsers,
            activeAssemblies,
            engagement,
            yearlyVotes,
            attention,
            new HashMap<>(),
            saldoAtual,
            pendingReceipts // Passando o valor real aqui
        );
    }
}